#pragma once

#include <cstddef>
#include <cstdint>

namespace dsqueez {

// Horizontal Lanczos-3 resample of an interleaved 8-bit RGB image.
//
//   src             : source pixel buffer, src_w * src_h * 3 bytes
//   src_w, src_h    : source dimensions
//   dst             : destination buffer, dst_w * src_h * 3 bytes (caller-owned)
//   dst_w           : destination width (typically lround(src_w * ratio))
//
// Height is preserved 1:1. Edge sampling clamps to the boundary pixels.
// Operates per-channel; gamma-correctness is not required for the anamorphic
// 1.33x case at 8-bit depth (any visible difference is below sensor noise).
void resample_horizontal_lanczos3(
    const uint8_t* src, int src_w, int src_h,
    uint8_t* dst, int dst_w
);

}  // namespace dsqueez
