// The whole pipeline: JPEG -> upright RGB -> Lanczos-3 horizontal stretch ->
// JPEG, with EXIF and ICC carried through untouched.

#include "dsqueez.h"
#include "jpeg_pipeline.h"
#include "lanczos.h"

#include <cmath>

#if defined(__ANDROID__)
#  include <android/log.h>
#  define LOG_TAG "dsqueez-native"
#  define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#  define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#else
#  include <cstdio>
#  define LOGI(...) do { std::fprintf(stderr, "[dsqueez] " __VA_ARGS__); std::fputc('\n', stderr); } while (0)
#  define LOGE(...) do { std::fprintf(stderr, "[dsqueez] " __VA_ARGS__); std::fputc('\n', stderr); } while (0)
#endif

namespace dsqueez {

namespace {

Orientation clamp_orientation(int o) {
    if (o < 1 || o > 8) return Orientation::Normal;
    return static_cast<Orientation>(o);
}

}  // namespace

Result desqueeze_jpeg(
    const uint8_t* src_bytes,
    size_t         src_len,
    float          ratio,
    int            quality,
    int            orientation
) {
    Result result;

    if (ratio <= 0.0f) {
        result.error = "ratio must be > 0";
        return result;
    }
    if (quality < 1 || quality > 100) quality = 95;

    DecodeResult decoded = decode_jpeg(src_bytes, src_len);
    if (!decoded.error.empty()) {
        result.error = std::move(decoded.error);
        return result;
    }
    // Raw sensor dimensions. The anamorphic squeeze is always along the sensor's
    // horizontal axis (the decoded buffer's width), regardless of how the phone
    // was held — so we stretch that axis FIRST, then bake EXIF orientation. For
    // a portrait capture (orientation 6/8) that rotation lands the stretch on
    // the upright image's vertical axis, which is exactly right.
    const int src_w = decoded.image.width;
    const int src_h = decoded.image.height;

    const int dst_w = static_cast<int>(std::lround(static_cast<float>(src_w) * ratio));
    if (dst_w <= 0) {
        result.error = "computed output width is zero";
        return result;
    }

    std::vector<uint8_t> resampled(static_cast<size_t>(dst_w) * src_h * 3);
    resample_horizontal_lanczos3(
        decoded.image.rgb.data(), src_w, src_h,
        resampled.data(), dst_w
    );

    // Free the source pixel buffer before the orientation pass; peak memory
    // matters on mobile and we don't need the source after this point.
    decoded.image.rgb.clear();
    decoded.image.rgb.shrink_to_fit();

    // Bake EXIF rotation into the stretched pixels so the output is upright and
    // the host can stamp Orientation = 1. Dimensions swap for 90° rotations.
    int out_w = dst_w;
    int out_h = src_h;
    apply_orientation(clamp_orientation(orientation), resampled, out_w, out_h);

    LOGI("desqueeze: sensor %dx%d -> %dx%d, upright %dx%d (ratio=%.4f, q=%d, exif=%zu, icc=%zu)",
         src_w, src_h, dst_w, src_h, out_w, out_h, ratio, quality,
         decoded.image.exif.size(), decoded.image.icc.size());

    EncodeResult encoded = encode_jpeg(
        resampled.data(), out_w, out_h,
        quality,
        decoded.image.exif,
        decoded.image.icc
    );
    if (!encoded.error.empty()) {
        result.error = std::move(encoded.error);
        return result;
    }

    result.bytes = std::move(encoded.bytes);
    return result;
}

}  // namespace dsqueez
