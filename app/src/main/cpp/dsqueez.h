#pragma once

#include <cstddef>
#include <cstdint>
#include <string>

namespace dsqueez {

enum class OutFormat : int {
    JPEG = 0,
    HEIC = 1,
};

struct Result {
    uint8_t* bytes = nullptr;
    size_t bytes_len = 0;
    std::string error;  // empty on success
};

Result desqueeze_buffer(
    const uint8_t* src,
    size_t src_len,
    float ratio,
    OutFormat out_format,
    int quality
);

void free_result_bytes(uint8_t* bytes);

}  // namespace dsqueez
