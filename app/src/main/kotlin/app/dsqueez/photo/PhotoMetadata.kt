package app.dsqueez.photo

import android.net.Uri

enum class SourceFormat(val mime: String, val extension: String) {
    JPEG("image/jpeg", "jpg"),
    UNKNOWN("application/octet-stream", "bin");

    companion object {
        fun fromMime(mime: String?): SourceFormat = when (mime?.lowercase()) {
            "image/jpeg", "image/jpg" -> JPEG
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
    /** Pixel dimensions as encoded in the file, before orientation is applied. */
    val pixelWidth: Int,
    val pixelHeight: Int,
    /** Original EXIF DateTimeOriginal in epoch millis; null if unparseable. */
    val captureTimeMillis: Long?,
    /** EXIF Orientation tag (1..8). Defaults to 1 (Normal). */
    val orientation: Int,
    /** Raw LensModel EXIF tag (e.g. "SIRUI 1.33x Anamorphic"); null if absent. */
    val lensModel: String?,
    /** A supported ratio derived from [lensModel] when a clear match exists. */
    val suggestedRatio: Float?,
) {
    val supported: Boolean get() = sourceFormat == SourceFormat.JPEG

    /** True iff EXIF orientation rotates 90° (swaps width/height when applied). */
    val swapsAxes: Boolean get() = orientation == 5 || orientation == 6 ||
                                   orientation == 7 || orientation == 8

    /** Width as the user perceives the upright image. */
    val uprightWidth: Int  get() = if (swapsAxes) pixelHeight else pixelWidth
    val uprightHeight: Int get() = if (swapsAxes) pixelWidth  else pixelHeight

    fun desqueezedWidth(ratio: Float): Int = (uprightWidth * ratio + 0.5f).toInt()
}
