#pragma once

#include <cstddef>
#include <cstdint>
#include <string>
#include <vector>

namespace dsqueez {

struct Result {
    std::vector<uint8_t> bytes;
    std::string          error;  // empty on success
};

// JPEG-in, JPEG-out. Horizontal Lanczos-3 stretch by `ratio`, height preserved.
//
//   src_bytes       : source JPEG bytes
//   src_len         : length in bytes
//   ratio           : horizontal scale (e.g. 1.33f)
//   quality         : JPEG quality 1..100 (95 is the calibrated default)
//   orientation     : EXIF orientation tag (1..8); pixels are rotated to upright
//                     before resampling, so the output is "Orientation = 1"
//
// EXIF (APP1) and ICC profile (APP2) are carried through from the source.
// The caller (Kotlin side) is responsible for patching EXIF geometry tags
// (PixelXDimension, ImageWidth, Orientation) on the published file.
Result desqueeze_jpeg(
    const uint8_t* src_bytes,
    size_t         src_len,
    float          ratio,
    int            quality,
    int            orientation
);

}  // namespace dsqueez
