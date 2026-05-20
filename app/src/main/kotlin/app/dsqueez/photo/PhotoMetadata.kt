package app.dsqueez.photo

import android.net.Uri

enum class SourceFormat(val mime: String, val extension: String) {
    JPEG("image/jpeg", "jpg"),
    HEIC("image/heif", "heic"),
    PNG("image/png", "png"),
    WEBP("image/webp", "webp"),
    UNKNOWN("application/octet-stream", "bin");

    companion object {
        fun fromMime(mime: String?): SourceFormat = when (mime?.lowercase()) {
            "image/jpeg", "image/jpg" -> JPEG
            "image/heic", "image/heif" -> HEIC
            "image/png" -> PNG
            "image/webp" -> WEBP
            else -> UNKNOWN
        }
    }
}

/** Everything we know about a source photo without decoding the full pixel grid. */
data class PhotoMetadata(
    val uri: Uri,
    val displayName: String,
    val sizeBytes: Long,
    val mimeType: String,
    val sourceFormat: SourceFormat,
    val pixelWidth: Int,
    val pixelHeight: Int,
    /** Original EXIF DateTimeOriginal in epoch millis; null if unparseable. */
    val captureTimeMillis: Long?,
    /** True if EXIF reports a non-identity orientation (we'll bake into pixels). */
    val hasRotation: Boolean,
) {
    val supported: Boolean get() = sourceFormat == SourceFormat.JPEG || sourceFormat == SourceFormat.HEIC

    fun desqueezedWidth(ratio: Float): Int = (pixelWidth * ratio + 0.5f).toInt()
}
