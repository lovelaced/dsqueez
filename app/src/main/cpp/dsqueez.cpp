// Single-purpose pipeline:
//   load_from_buffer -> affine(ratio, 1.0, Lanczos-3) -> jpegsave/heifsave to buffer
//
// libvips streams tile-by-tile, so peak RSS stays well below the full bitmap size.
// EXIF and ICC profile are carried through automatically by libvips when the
// source has them; we explicitly opt OUT of strip=TRUE in the save call.

#include "dsqueez.h"

#include <vips/vips.h>
#include <android/log.h>
#include <cstring>
#include <string>

#define LOG_TAG "dsqueez-native"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace dsqueez {

namespace {
bool g_vips_initialized = false;

bool ensure_vips_initialized() {
    if (g_vips_initialized) return true;
    if (VIPS_INIT("dsqueez") != 0) {
        LOGE("vips_init failed: %s", vips_error_buffer());
        vips_error_clear();
        return false;
    }
    g_vips_initialized = true;
    return true;
}
}  // namespace

Result desqueeze_buffer(
    const uint8_t* src,
    size_t src_len,
    float ratio,
    OutFormat out_format,
    int quality
) {
    Result result{};

    if (!ensure_vips_initialized()) {
        result.error = "libvips initialization failed";
        return result;
    }

    VipsImage* in = vips_image_new_from_buffer(
        src, src_len, "",
        "access", VIPS_ACCESS_SEQUENTIAL,
        nullptr
    );
    if (!in) {
        result.error = std::string("decode failed: ") + vips_error_buffer();
        vips_error_clear();
        return result;
    }

    // Bake EXIF orientation into pixels before any further processing. We force
    // Orientation=1 on the output (see MediaStoreSaver), so without this step a
    // portrait JPEG (Orientation=6) would land sideways in Lightroom.
    VipsImage* upright = nullptr;
    if (vips_autorot(in, &upright, nullptr) != 0) {
        result.error = std::string("autorot failed: ") + vips_error_buffer();
        vips_error_clear();
        g_object_unref(in);
        return result;
    }
    g_object_unref(in);

    VipsImage* out = nullptr;
    VipsInterpolate* lanczos = vips_interpolate_new("lanczos3");
    if (!lanczos) {
        g_object_unref(upright);
        result.error = "could not create lanczos3 interpolator";
        return result;
    }

    // vips_affine takes a 2x2 matrix: [a b ; c d]. For a horizontal stretch
    // by `ratio` and vertical identity, that's [ratio 0 ; 0 1].
    int affine_rc = vips_affine(
        upright, &out,
        (double)ratio, 0.0,
        0.0,          1.0,
        "interpolate", lanczos,
        nullptr
    );

    g_object_unref(lanczos);
    g_object_unref(upright);

    if (affine_rc != 0 || !out) {
        result.error = std::string("affine failed: ") + vips_error_buffer();
        vips_error_clear();
        return result;
    }

    void* buf = nullptr;
    size_t buf_len = 0;

    int save_rc;
    if (out_format == OutFormat::JPEG) {
        save_rc = vips_jpegsave_buffer(out, &buf, &buf_len,
            "Q", quality,
            "strip", FALSE,                 // keep EXIF + ICC
            "optimize_coding", TRUE,
            "trellis_quant", TRUE,
            "subsample_mode", VIPS_FOREIGN_SUBSAMPLE_OFF,
            nullptr);
    } else {
        save_rc = vips_heifsave_buffer(out, &buf, &buf_len,
            "Q", quality,
            "compression", VIPS_FOREIGN_HEIF_COMPRESSION_HEVC,
            nullptr);
    }

    g_object_unref(out);

    if (save_rc != 0 || !buf) {
        result.error = std::string("encode failed: ") + vips_error_buffer();
        vips_error_clear();
        if (buf) g_free(buf);
        return result;
    }

    result.bytes = reinterpret_cast<uint8_t*>(buf);
    result.bytes_len = buf_len;
    return result;
}

void free_result_bytes(uint8_t* bytes) {
    if (bytes) g_free(bytes);
}

}  // namespace dsqueez
