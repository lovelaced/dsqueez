#include "lanczos.h"

#include <algorithm>
#include <cmath>
#include <vector>

namespace dsqueez {

namespace {

constexpr int   kRadius = 3;
constexpr float kPi     = 3.14159265358979323846f;

inline float sinc(float x) {
    if (x == 0.0f) return 1.0f;
    const float px = kPi * x;
    return std::sin(px) / px;
}

inline float lanczos3(float x) {
    if (x == 0.0f) return 1.0f;
    if (x <= -kRadius || x >= kRadius) return 0.0f;
    return sinc(x) * sinc(x / kRadius);
}

// Precomputed per-output-column sampling plan: which source pixels to read,
// what weight each carries. Computing weights once per output column (rather
// than per row) is the standard separable-resampler optimization — for a
// 6000x4000 source upscaled 1.33x, that's ~7980 weight tables instead of
// ~32 million per-pixel evaluations.
struct ColumnPlan {
    int   first;            // first source column read
    int   count;            // number of source columns read (up to 2*kRadius)
    float weights[2 * kRadius + 1];
};

std::vector<ColumnPlan> build_column_plans(int src_w, int dst_w) {
    std::vector<ColumnPlan> plans(static_cast<size_t>(dst_w));

    // Standard upscale-friendly source-position formula (cell-center aligned).
    const float scale = static_cast<float>(src_w) / static_cast<float>(dst_w);

    for (int xo = 0; xo < dst_w; ++xo) {
        // Map output column center to source position.
        const float src_pos = (static_cast<float>(xo) + 0.5f) * scale - 0.5f;
        const int   center  = static_cast<int>(std::floor(src_pos));

        ColumnPlan& p = plans[static_cast<size_t>(xo)];
        p.first = center - (kRadius - 1);
        p.count = 2 * kRadius;

        float wsum = 0.0f;
        for (int k = 0; k < p.count; ++k) {
            const int   src_x = p.first + k;
            const float dx    = static_cast<float>(src_x) - src_pos;
            const float w     = lanczos3(dx);
            p.weights[k]      = w;
            wsum             += w;
        }
        // Normalize so weights sum to 1, preserving overall image brightness.
        if (wsum != 0.0f) {
            const float inv = 1.0f / wsum;
            for (int k = 0; k < p.count; ++k) p.weights[k] *= inv;
        }
    }
    return plans;
}

inline uint8_t clamp_u8(float v) {
    if (v <= 0.0f) return 0;
    if (v >= 255.0f) return 255;
    return static_cast<uint8_t>(v + 0.5f);
}

inline int clamp_src_x(int x, int src_w) {
    if (x < 0) return 0;
    if (x >= src_w) return src_w - 1;
    return x;
}

}  // namespace

void resample_horizontal_lanczos3(
    const uint8_t* src, int src_w, int src_h,
    uint8_t* dst, int dst_w
) {
    const std::vector<ColumnPlan> plans = build_column_plans(src_w, dst_w);

    const size_t src_row_stride = static_cast<size_t>(src_w) * 3;
    const size_t dst_row_stride = static_cast<size_t>(dst_w) * 3;

    for (int y = 0; y < src_h; ++y) {
        const uint8_t* src_row = src + static_cast<size_t>(y) * src_row_stride;
        uint8_t*       dst_row = dst + static_cast<size_t>(y) * dst_row_stride;

        for (int xo = 0; xo < dst_w; ++xo) {
            const ColumnPlan& p = plans[static_cast<size_t>(xo)];

            float r = 0.0f, g = 0.0f, b = 0.0f;
            for (int k = 0; k < p.count; ++k) {
                const int src_x = clamp_src_x(p.first + k, src_w);
                const uint8_t* px = src_row + static_cast<size_t>(src_x) * 3;
                const float w = p.weights[k];
                r += w * static_cast<float>(px[0]);
                g += w * static_cast<float>(px[1]);
                b += w * static_cast<float>(px[2]);
            }

            uint8_t* out = dst_row + static_cast<size_t>(xo) * 3;
            out[0] = clamp_u8(r);
            out[1] = clamp_u8(g);
            out[2] = clamp_u8(b);
        }
    }
}

}  // namespace dsqueez
