package app.dsqueez.nativebridge

import android.util.Log

/**
 * Native JPEG resampler. Lanczos-3 horizontal stretch built on libjpeg-turbo,
 * compiled into libdsqueez.so via the project's own CMake. EXIF (APP1) and ICC
 * profile (APP2) are carried through from the source untouched.
 */
object Resampler {
    private const val TAG = "Resampler"

    @Volatile
    private var loaded: Boolean = false

    fun tryLoad() {
        if (loaded) return
        try {
            System.loadLibrary("dsqueez")
            loaded = true
            Log.i(TAG, "libdsqueez.so loaded")
        } catch (t: UnsatisfiedLinkError) {
            Log.e(TAG, "libdsqueez.so failed to load — native pipeline unavailable", t)
        } catch (t: Throwable) {
            Log.e(TAG, "unexpected error loading libdsqueez", t)
        }
    }

    val isAvailable: Boolean get() = loaded

    /**
     * Desqueeze a JPEG by horizontal scale [ratio] using Lanczos-3.
     *
     * @param srcBytes    encoded source JPEG bytes
     * @param ratio       horizontal scale factor (e.g. 1.33f)
     * @param quality     JPEG output quality, 1..100 (95 is the calibrated default)
     * @param orientation EXIF orientation of the source (1..8). Pixels are rotated
     *                    to upright before resampling, so the output is logically
     *                    Orientation = 1. The host is responsible for patching the
     *                    Orientation EXIF tag on the saved file.
     * @return encoded JPEG bytes with EXIF + ICC carried through
     * @throws ResamplerException on any libjpeg / encode error
     */
    @Throws(ResamplerException::class)
    external fun desqueezeBytes(
        srcBytes: ByteArray,
        ratio: Float,
        quality: Int,
        orientation: Int,
    ): ByteArray
}

class ResamplerException(message: String) : RuntimeException(message)
