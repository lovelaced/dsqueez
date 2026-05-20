package app.dsqueez.photo

import android.content.Context
import android.net.Uri
import app.dsqueez.nativebridge.Resampler
import app.dsqueez.nativebridge.ResamplerException
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

    private const val DEFAULT_JPEG_QUALITY = 95

    /**
     * Single-photo desqueeze. Reads the source bytes, hands them to the native
     * resampler with the source orientation, then publishes the result via
     * MediaStore in the dsqueez album.
     */
    suspend fun process(
        context: Context,
        metadata: PhotoMetadata,
        ratio: Float,
    ): SaveResult = withContext(Dispatchers.Default) {
        if (!metadata.supported) {
            return@withContext SaveResult.Failure(FailureReason.UNSUPPORTED_FORMAT)
        }
        if (!Resampler.isAvailable) {
            return@withContext SaveResult.Failure(FailureReason.NATIVE_ENGINE_MISSING)
        }

        val srcBytes = runCatching {
            context.contentResolver.openInputStream(metadata.uri)?.use { it.readBytes() }
        }.getOrNull() ?: return@withContext SaveResult.Failure(FailureReason.READ_FAILED)

        val outBytes = try {
            Resampler.desqueezeBytes(
                srcBytes = srcBytes,
                ratio = ratio,
                quality = DEFAULT_JPEG_QUALITY,
                orientation = metadata.orientation,
            )
        } catch (t: ResamplerException) {
            return@withContext SaveResult.Failure(FailureReason.PROCESS_FAILED, t)
        } catch (t: Throwable) {
            return@withContext SaveResult.Failure(FailureReason.PROCESS_FAILED, t)
        }

        val newWidth  = metadata.desqueezedWidth(ratio)
        val newHeight = metadata.uprightHeight

        val savedUri = runCatching {
            MediaStoreSaver.publish(
                context = context,
                bytes = outBytes,
                metadata = metadata,
                newWidth = newWidth,
                newHeight = newHeight,
            )
        }.getOrElse { t ->
            return@withContext SaveResult.Failure(FailureReason.WRITE_FAILED, t)
        }

        SaveResult.Success(savedUri, newWidth, newHeight)
    }
}
