package app.dsqueez.photo

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorSpace
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.exifinterface.media.ExifInterface
import app.dsqueez.ui.components.SUPPORTED_RATIOS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object PhotoSource {

    private const val PREVIEW_MAX_LONGEST_SIDE = 2048

    suspend fun readMetadata(context: Context, uri: Uri): PhotoMetadata = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val (displayName, sizeBytes) = queryNameAndSize(resolver, uri)
        val mime = resolver.getType(uri) ?: "application/octet-stream"
        val format = SourceFormat.fromMime(mime)

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }

        val exif = runCatching {
            resolver.openInputStream(uri)?.use { ExifInterface(it) }
        }.getOrNull()

        val orientation = exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            ?: ExifInterface.ORIENTATION_NORMAL
        val captureTime = exif?.let(::extractCaptureTimeMs)
        val lensModel = exif?.getAttribute(ExifInterface.TAG_LENS_MODEL)?.trim()?.takeIf { it.isNotEmpty() }
        val suggestedRatio = lensModel?.let(::extractRatioFromLens)

        PhotoMetadata(
            uri = uri,
            displayName = displayName,
            sizeBytes = sizeBytes,
            mimeType = mime,
            sourceFormat = format,
            pixelWidth = bounds.outWidth,
            pixelHeight = bounds.outHeight,
            captureTimeMillis = captureTime,
            orientation = orientation,
            lensModel = lensModel,
            suggestedRatio = suggestedRatio,
        )
    }

    /**
     * Pull a supported anamorphic ratio out of a lens model string.
     *
     * Anamorphic lens makers announce the squeeze factor in the model name —
     * "SIRUI 1.33x Anamorphic", "Vazen 1.8x Anamorphic", "Cooke /i 2x", etc.
     * Match the numeric portion, then snap to the closest supported ratio.
     * Reject matches further than 0.05 from any supported value so non-
     * anamorphic lens names (e.g. "S-R50/65") don't get accidentally tagged.
     */
    private val LensRatioRegex = Regex("""(\d+(?:\.\d+)?)\s*x""", RegexOption.IGNORE_CASE)

    private fun extractRatioFromLens(lensModel: String): Float? {
        val raw = LensRatioRegex.find(lensModel)?.groupValues?.get(1)?.toFloatOrNull() ?: return null
        val closest = SUPPORTED_RATIOS.minByOrNull { abs(it - raw) } ?: return null
        return closest.takeIf { abs(it - raw) < 0.05f }
    }

    /**
     * Decode a downsampled preview bitmap, rotated upright per the EXIF
     * [orientation]. Targets [PREVIEW_MAX_LONGEST_SIDE] for the longest dimension
     * so we never hold a 24MP bitmap in memory just to show the user what they're
     * working with. BitmapFactory ignores the orientation tag, so we apply it here
     * — the preview must match the upright output the save pipeline produces.
     */
    suspend fun decodePreview(
        context: Context,
        uri: Uri,
        originalWidth: Int,
        originalHeight: Int,
        orientation: Int = ExifInterface.ORIENTATION_NORMAL,
    ): Bitmap? =
        withContext(Dispatchers.IO) {
            val longest = maxOf(originalWidth, originalHeight).coerceAtLeast(1)
            val sample = computeSampleSize(longest, PREVIEW_MAX_LONGEST_SIDE)

            val opts = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    inPreferredColorSpace = ColorSpace.get(ColorSpace.Named.DISPLAY_P3)
                }
            }

            val raw = context.contentResolver.openInputStream(uri).use { input: InputStream? ->
                BitmapFactory.decodeStream(input, null, opts)
            } ?: return@withContext null

            applyOrientation(raw, orientation)
        }

    /** Rotate/flip [bitmap] to upright per an EXIF orientation tag (1..8). */
    private fun applyOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val m = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180      -> m.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL   -> { m.setRotate(180f); m.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSPOSE       -> { m.setRotate(90f);  m.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_90       -> m.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE      -> { m.setRotate(270f); m.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_270      -> m.setRotate(270f)
            else -> return bitmap  // Normal or undefined
        }
        return runCatching {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
                .also { if (it != bitmap) bitmap.recycle() }
        }.getOrDefault(bitmap)
    }

    private fun queryNameAndSize(resolver: ContentResolver, uri: Uri): Pair<String, Long> {
        var name = uri.lastPathSegment ?: "photo"
        var size = 0L
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val nameIdx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }
                val sizeIdx = c.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 }
                if (nameIdx != null && !c.isNull(nameIdx)) name = c.getString(nameIdx) ?: name
                if (sizeIdx != null && !c.isNull(sizeIdx)) size = c.getLong(sizeIdx)
            }
        }
        return name to size
    }

    private fun extractCaptureTimeMs(exif: ExifInterface): Long? {
        val raw = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
            ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
            ?: return null
        return runCatching {
            val fmt = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)
            fmt.timeZone = TimeZone.getDefault()
            fmt.parse(raw)?.time
        }.getOrNull()
    }

    private fun computeSampleSize(srcLongest: Int, targetLongest: Int): Int {
        if (srcLongest <= targetLongest) return 1
        var s = 1
        while ((srcLongest / (s * 2)) >= targetLongest) s *= 2
        return s
    }
}
