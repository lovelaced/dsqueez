#pragma once

#include <cstddef>
#include <cstdint>
#include <string>
#include <vector>

namespace dsqueez {

// EXIF Orientation tag values (TIFF spec). We accept 1, 3, 6, 8 from cameras.
// Values 2, 4, 5, 7 add a mirror — supported but extremely rare in practice.
enum class Orientation : int {
    Normal       = 1,
    FlipH        = 2,
    Rotate180    = 3,
    FlipV        = 4,
    Transpose    = 5,   // rotate 90 CW + flip H
    Rotate90CW   = 6,
    Transverse   = 7,   // rotate 90 CCW + flip H
    Rotate90CCW  = 8,
};

struct DecodedImage {
    std::vector<uint8_t> rgb;     // tightly packed RGB, width*height*3 bytes
    int width  = 0;
    int height = 0;

    // Raw bytes of the source APP1 EXIF segment (starts with "Exif\0\0").
    // Empty if the source had no EXIF.
    std::vector<uint8_t> exif;

    // Reassembled ICC profile (libjpeg combines multi-segment ICC for us).
    // Empty if the source had no ICC profile.
    std::vector<uint8_t> icc;
};

struct DecodeResult {
    DecodedImage image;
    std::string  error;  // empty on success
};

// Decode a JPEG from memory. EXIF and ICC are captured for later passthrough;
// the image is returned upright (orientation already baked into pixels).
DecodeResult decode_jpeg(const uint8_t* src, size_t src_len, Orientation orientation);

struct EncodeResult {
    std::vector<uint8_t> bytes;
    std::string          error;  // empty on success
};

// Encode RGB to JPEG at the given quality, embedding the supplied EXIF +
// ICC blobs verbatim. The caller is responsible for patching EXIF tags
// (Orientation, PixelXDimension, etc.) on the host side after writing.
EncodeResult encode_jpeg(
    const uint8_t* rgb, int width, int height,
    int quality,
    const std::vector<uint8_t>& exif,
    const std::vector<uint8_t>& icc
);

}  // namespace dsqueez
