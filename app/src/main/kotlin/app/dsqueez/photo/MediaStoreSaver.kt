package app.dsqueez.photo

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import app.dsqueez.nativebridge.Vips

object MediaStoreSaver {

    private const val ALBUM_DIR = "Pictures/dsqueez"

    /**
     * Publish [bytes] into MediaStore under the dsqueez album.
     *
     * Uses IS_PENDING flow so the file never appears half-written in Google Photos.
     * Propagates DATE_TAKEN from the source EXIF so the desqueezed copy sorts
     * chronologically next to the original.
     */
    fun publish(
        context: Context,
        bytes: ByteArray,
        metadata: PhotoMetadata,
        outFormat: Int,
        newWidth: Int,
        newHeight: Int,
    ): Uri {
        val (mime, ext) = when (outFormat) {
            Vips.OutFormat.JPEG -> "image/jpeg" to "jpg"
            Vips.OutFormat.HEIC -> "image/heif" to "heic"
            else -> "image/jpeg" to "jpg"
        }

        val baseName = metadata.displayName
            .substringBeforeLast('.')
            .ifBlank { "IMG_${System.currentTimeMillis()}" }
        val targetName = "${baseName}_dsq.$ext"
        val takenAt = metadata.captureTimeMillis ?: System.currentTimeMillis()

        val resolver = context.contentResolver
        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, targetName)
            put(MediaStore.Images.Media.MIME_TYPE, mime)
            put(MediaStore.Images.Media.RELATIVE_PATH, ALBUM_DIR)
            put(MediaStore.Images.Media.IS_PENDING, 1)
            put(MediaStore.Images.Media.DATE_TAKEN, takenAt)
            put(MediaStore.Images.Media.WIDTH, newWidth)
            put(MediaStore.Images.Media.HEIGHT, newHeight)
        }

        val uri = resolver.insert(collection, values)
            ?: error("MediaStore insert returned null")

        try {
            resolver.openOutputStream(uri, "w").use { os ->
                requireNotNull(os) { "Could not open output stream for $uri" }
                os.write(bytes)
                os.flush()
            }

            // Belt-and-braces EXIF geometry patch — libvips writes EXIF natively,
            // but we guarantee the geometry tags + orientation are correct.
            if (outFormat == Vips.OutFormat.JPEG) {
                runCatching {
                    resolver.openFileDescriptor(uri, "rw")?.use { pfd ->
                        val exif = ExifInterface(pfd.fileDescriptor)
                        exif.setAttribute(ExifInterface.TAG_PIXEL_X_DIMENSION, newWidth.toString())
                        exif.setAttribute(ExifInterface.TAG_IMAGE_WIDTH, newWidth.toString())
                        exif.setAttribute(ExifInterface.TAG_PIXEL_Y_DIMENSION, newHeight.toString())
                        exif.setAttribute(ExifInterface.TAG_IMAGE_LENGTH, newHeight.toString())
                        exif.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
                        exif.saveAttributes()
                    }
                }
            }

            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } catch (t: Throwable) {
            runCatching { resolver.delete(uri, null, null) }
            throw t
        }

        return uri
    }
}
