package app.dsqueez.photo

import android.content.Context
import android.net.Uri
import app.dsqueez.nativebridge.DesqueezException
import app.dsqueez.nativebridge.Vips
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface SaveResult {
    data class Success(val uri: Uri, val outputWidth: Int, val outputHeight: Int) : SaveResult
    data class Failure(val reason: FailureReason, val throwable: Throwable? = null) : SaveResult
}

enum class FailureReason {
    UNSUPPORTED_FORMAT,
    NATIVE_ENGINE_MISSING,
    READ_FAILED,
    PROCESS_FAILED,
    WRITE_FAILED,
}

object Pipeline {

    /**
     * The full single-photo pipeline:
     *   1. Read source bytes from the content URI.
     *   2. Hand to libvips for desqueeze + re-encode (EXIF + ICC carried natively).
     *   3. Apply MediaStore IS_PENDING flow, write to the dsqueez album.
     *   4. Patch geometry EXIF tags via AndroidX ExifInterface (belt + braces).
     */
    suspend fun process(
        context: Context,
        metadata: PhotoMetadata,
        ratio: Float,
        exportAsJpeg: Boolean,
    ): SaveResult = withContext(Dispatchers.Default) {
        if (!metadata.supported) {
            return@withContext SaveResult.Failure(FailureReason.UNSUPPORTED_FORMAT)
        }
        if (!Vips.isAvailable) {
            return@withContext SaveResult.Failure(FailureReason.NATIVE_ENGINE_MISSING)
        }

        val srcBytes = runCatching {
            context.contentResolver.openInputStream(metadata.uri)?.use { it.readBytes() }
        }.getOrNull() ?: return@withContext SaveResult.Failure(FailureReason.READ_FAILED)

        val outFormat = if (exportAsJpeg) Vips.OutFormat.JPEG else when (metadata.sourceFormat) {
            SourceFormat.HEIC -> Vips.OutFormat.HEIC
            else -> Vips.OutFormat.JPEG
        }

        val quality = if (outFormat == Vips.OutFormat.JPEG) 95 else 90

        val outBytes = runCatching {
            Vips.desqueezeBytes(srcBytes, ratio, outFormat, quality)
        }.getOrElse { t ->
            return@withContext SaveResult.Failure(
                if (t is DesqueezException) FailureReason.PROCESS_FAILED else FailureReason.PROCESS_FAILED,
                t,
            )
        }

        val newWidth = metadata.desqueezedWidth(ratio)
        val newHeight = metadata.pixelHeight

        val savedUri = runCatching {
            MediaStoreSaver.publish(
                context = context,
                bytes = outBytes,
                metadata = metadata,
                outFormat = outFormat,
                newWidth = newWidth,
                newHeight = newHeight,
            )
        }.getOrElse { t ->
            return@withContext SaveResult.Failure(FailureReason.WRITE_FAILED, t)
        }

        SaveResult.Success(savedUri, newWidth, newHeight)
    }
}
