package app.dsqueez.nativebridge

import android.util.Log

/**
 * JNI surface to libvips-backed desqueeze.
 *
 * The native libraries (libvips.so + transitive deps + libdsqueez.so) must be vendored
 * under app/src/main/jniLibs/arm64-v8a/. Until they're in place, [isAvailable] returns
 * false and the UI falls back to a placeholder save path.
 */
object Vips {
    private const val TAG = "Vips"

    @Volatile
    private var loaded: Boolean = false

    fun tryLoad() {
        if (loaded) return
        try {
            System.loadLibrary("dsqueez")
            loaded = true
            Log.i(TAG, "libdsqueez.so loaded; libvips backend online")
        } catch (t: UnsatisfiedLinkError) {
            Log.w(TAG, "libdsqueez.so not present yet — vendor prebuilts to enable processing", t)
        } catch (t: Throwable) {
            Log.e(TAG, "unexpected error loading libdsqueez", t)
        }
    }

    val isAvailable: Boolean get() = loaded

    /**
     * Desqueeze a still by horizontal scale [ratio] using libvips Lanczos-3.
     *
     * @param srcBytes encoded source bytes (JPEG or HEIC)
     * @param ratio horizontal scale factor (e.g. 1.33f)
     * @param outFormat one of [OutFormat]
     * @param quality 1..100 (JPEG q95 / HEIC q90 are the calibrated defaults)
     * @return encoded output bytes, EXIF + ICC preserved by libvips
     * @throws DesqueezException on any libvips error
     */
    @Throws(DesqueezException::class)
    external fun desqueezeBytes(
        srcBytes: ByteArray,
        ratio: Float,
        outFormat: Int,
        quality: Int,
    ): ByteArray

    object OutFormat {
        const val JPEG = 0
        const val HEIC = 1
    }
}

class DesqueezException(message: String) : RuntimeException(message)
