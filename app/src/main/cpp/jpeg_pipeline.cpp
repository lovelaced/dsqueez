#include "jpeg_pipeline.h"

#include <cstdio>
#include <cstring>
#include <csetjmp>
#include <memory>

extern "C" {
#include <jpeglib.h>
#include <jerror.h>
}

namespace dsqueez {

namespace {

// ---- libjpeg error trap ----------------------------------------------------
//
// libjpeg's default error_exit() calls exit(). We replace it with longjmp so
// errors become recoverable failures instead of process suicides.

struct ErrorMgr {
    jpeg_error_mgr pub;
    jmp_buf        setjmp_buffer;
    char           message[JMSG_LENGTH_MAX];
};

void error_exit_jump(j_common_ptr cinfo) {
    ErrorMgr* err = reinterpret_cast<ErrorMgr*>(cinfo->err);
    (*cinfo->err->format_message)(cinfo, err->message);
    longjmp(err->setjmp_buffer, 1);
}

// ---- EXIF / ICC marker handling -------------------------------------------

constexpr int  kAppExif = JPEG_APP0 + 1;   // APP1
constexpr int  kAppIcc  = JPEG_APP0 + 2;   // APP2
constexpr char kExifId[] = "Exif\0";       // 6 bytes total incl. trailing \0

bool is_exif_marker(const jpeg_marker_struct* m) {
    return m->marker == kAppExif
        && m->data_length >= 6
        && std::memcmp(m->data, kExifId, 6) == 0;
}

std::vector<uint8_t> extract_exif(j_decompress_ptr cinfo) {
    for (jpeg_marker_struct* m = cinfo->marker_list; m != nullptr; m = m->next) {
        if (is_exif_marker(m)) {
            return std::vector<uint8_t>(m->data, m->data + m->data_length);
        }
    }
    return {};
}

std::vector<uint8_t> extract_icc(j_decompress_ptr cinfo) {
    JOCTET* icc_data = nullptr;
    unsigned int icc_len = 0;
    if (jpeg_read_icc_profile(cinfo, &icc_data, &icc_len) && icc_data != nullptr) {
        std::vector<uint8_t> out(icc_data, icc_data + icc_len);
        free(icc_data);
        return out;
    }
    return {};
}

// ---- Orientation transforms ------------------------------------------------
//
// Each function reads from `src` (width=sw, height=sh) and writes to `dst`,
// returning the new (width, height) via out params. Stride is width*3 (tight).
//
// Inputs are guaranteed to be 8-bit RGB.

void rotate_180(const uint8_t* src, int sw, int sh, uint8_t* dst) {
    const size_t pixels = static_cast<size_t>(sw) * sh;
    for (size_t i = 0; i < pixels; ++i) {
        const size_t s = (pixels - 1 - i) * 3;
        const size_t d = i * 3;
        dst[d + 0] = src[s + 0];
        dst[d + 1] = src[s + 1];
        dst[d + 2] = src[s + 2];
    }
}

void flip_horizontal(const uint8_t* src, int sw, int sh, uint8_t* dst) {
    for (int y = 0; y < sh; ++y) {
        const uint8_t* srow = src + static_cast<size_t>(y) * sw * 3;
        uint8_t*       drow = dst + static_cast<size_t>(y) * sw * 3;
        for (int x = 0; x < sw; ++x) {
            const uint8_t* sp = srow + (sw - 1 - x) * 3;
            uint8_t*       dp = drow + x * 3;
            dp[0] = sp[0]; dp[1] = sp[1]; dp[2] = sp[2];
        }
    }
}

void flip_vertical(const uint8_t* src, int sw, int sh, uint8_t* dst) {
    const size_t row_bytes = static_cast<size_t>(sw) * 3;
    for (int y = 0; y < sh; ++y) {
        const uint8_t* srow = src + static_cast<size_t>(sh - 1 - y) * row_bytes;
        uint8_t*       drow = dst + static_cast<size_t>(y) * row_bytes;
        std::memcpy(drow, srow, row_bytes);
    }
}

// Rotate 90 CW: out[y][x] = in[sh-1-x][y]. New width = sh, new height = sw.
void rotate_90_cw(const uint8_t* src, int sw, int sh, uint8_t* dst) {
    const int dw = sh;
    const int dh = sw;
    for (int y = 0; y < dh; ++y) {
        uint8_t* drow = dst + static_cast<size_t>(y) * dw * 3;
        for (int x = 0; x < dw; ++x) {
            const int sx = y;
            const int sy = sh - 1 - x;
            const uint8_t* sp = src + (static_cast<size_t>(sy) * sw + sx) * 3;
            uint8_t*       dp = drow + x * 3;
            dp[0] = sp[0]; dp[1] = sp[1]; dp[2] = sp[2];
        }
    }
}

// Rotate 90 CCW: out[y][x] = in[x][sw-1-y]. New width = sh, new height = sw.
void rotate_90_ccw(const uint8_t* src, int sw, int sh, uint8_t* dst) {
    const int dw = sh;
    const int dh = sw;
    for (int y = 0; y < dh; ++y) {
        uint8_t* drow = dst + static_cast<size_t>(y) * dw * 3;
        for (int x = 0; x < dw; ++x) {
            const int sx = sw - 1 - y;
            const int sy = x;
            const uint8_t* sp = src + (static_cast<size_t>(sy) * sw + sx) * 3;
            uint8_t*       dp = drow + x * 3;
            dp[0] = sp[0]; dp[1] = sp[1]; dp[2] = sp[2];
        }
    }
}

}  // namespace

void apply_orientation(
    Orientation o,
    std::vector<uint8_t>& buf,
    int& w, int& h
) {
    if (o == Orientation::Normal) return;

    std::vector<uint8_t> tmp(buf.size());
    int nw = w, nh = h;

    switch (o) {
        case Orientation::FlipH:        flip_horizontal(buf.data(), w, h, tmp.data()); break;
        case Orientation::Rotate180:    rotate_180     (buf.data(), w, h, tmp.data()); break;
        case Orientation::FlipV:        flip_vertical  (buf.data(), w, h, tmp.data()); break;
        case Orientation::Rotate90CW:   rotate_90_cw   (buf.data(), w, h, tmp.data()); nw = h; nh = w; break;
        case Orientation::Rotate90CCW:  rotate_90_ccw  (buf.data(), w, h, tmp.data()); nw = h; nh = w; break;
        case Orientation::Transpose: {
            // 90 CW then flip H = transpose along main diagonal
            std::vector<uint8_t> mid(buf.size());
            rotate_90_cw(buf.data(), w, h, mid.data());
            flip_horizontal(mid.data(), h, w, tmp.data());
            nw = h; nh = w;
            break;
        }
        case Orientation::Transverse: {
            // 90 CCW then flip H = transpose along anti-diagonal
            std::vector<uint8_t> mid(buf.size());
            rotate_90_ccw(buf.data(), w, h, mid.data());
            flip_horizontal(mid.data(), h, w, tmp.data());
            nw = h; nh = w;
            break;
        }
        default:
            return;
    }

    buf.swap(tmp);
    w = nw;
    h = nh;
}

DecodeResult decode_jpeg(const uint8_t* src, size_t src_len) {
    DecodeResult result;

    jpeg_decompress_struct cinfo{};
    ErrorMgr               err{};
    cinfo.err = jpeg_std_error(&err.pub);
    err.pub.error_exit = &error_exit_jump;

    if (setjmp(err.setjmp_buffer)) {
        jpeg_destroy_decompress(&cinfo);
        result.error = std::string("decode failed: ") + err.message;
        return result;
    }

    jpeg_create_decompress(&cinfo);
    jpeg_mem_src(&cinfo, src, src_len);

    // Retain APP1 (EXIF) and APP2 (ICC) markers so we can copy them through.
    jpeg_save_markers(&cinfo, kAppExif, 0xFFFF);
    jpeg_save_markers(&cinfo, kAppIcc,  0xFFFF);

    if (jpeg_read_header(&cinfo, TRUE) != JPEG_HEADER_OK) {
        jpeg_destroy_decompress(&cinfo);
        result.error = "not a recognizable JPEG";
        return result;
    }

    cinfo.out_color_space = JCS_RGB;
    if (!jpeg_start_decompress(&cinfo)) {
        jpeg_destroy_decompress(&cinfo);
        result.error = "jpeg_start_decompress failed";
        return result;
    }

    const int w = static_cast<int>(cinfo.output_width);
    const int h = static_cast<int>(cinfo.output_height);
    const int channels = cinfo.output_components;  // expected 3

    if (channels != 3) {
        jpeg_destroy_decompress(&cinfo);
        result.error = "unsupported channel count " + std::to_string(channels);
        return result;
    }

    DecodedImage& img = result.image;
    img.width  = w;
    img.height = h;
    img.rgb.resize(static_cast<size_t>(w) * h * 3);
    img.exif = extract_exif(&cinfo);
    img.icc  = extract_icc(&cinfo);

    const size_t row_stride = static_cast<size_t>(w) * 3;
    while (cinfo.output_scanline < cinfo.output_height) {
        uint8_t* row_ptr = img.rgb.data() + static_cast<size_t>(cinfo.output_scanline) * row_stride;
        JSAMPROW rows[1] = { row_ptr };
        jpeg_read_scanlines(&cinfo, rows, 1);
    }

    jpeg_finish_decompress(&cinfo);
    jpeg_destroy_decompress(&cinfo);

    // Returned in raw sensor orientation. The caller resamples along the sensor
    // axis (where the anamorphic squeeze lives) before baking EXIF rotation.
    return result;
}

EncodeResult encode_jpeg(
    const uint8_t* rgb, int width, int height,
    int quality,
    const std::vector<uint8_t>& exif,
    const std::vector<uint8_t>& icc
) {
    EncodeResult result;

    jpeg_compress_struct cinfo{};
    ErrorMgr             err{};
    cinfo.err = jpeg_std_error(&err.pub);
    err.pub.error_exit = &error_exit_jump;

    unsigned char* out_buf = nullptr;
    unsigned long  out_len = 0;

    if (setjmp(err.setjmp_buffer)) {
        jpeg_destroy_compress(&cinfo);
        if (out_buf) free(out_buf);
        result.error = std::string("encode failed: ") + err.message;
        return result;
    }

    jpeg_create_compress(&cinfo);
    jpeg_mem_dest(&cinfo, &out_buf, &out_len);

    cinfo.image_width      = static_cast<JDIMENSION>(width);
    cinfo.image_height     = static_cast<JDIMENSION>(height);
    cinfo.input_components = 3;
    cinfo.in_color_space   = JCS_RGB;

    jpeg_set_defaults(&cinfo);
    jpeg_set_quality(&cinfo, quality, TRUE /* force_baseline */);
    cinfo.optimize_coding = TRUE;
    // Progressive output: smaller file, photographer doesn't notice decode cost.
    jpeg_simple_progression(&cinfo);
    // Full-resolution chroma at q95 — anamorphic detail deserves it.
    if (quality >= 90) {
        cinfo.comp_info[0].h_samp_factor = 1;
        cinfo.comp_info[0].v_samp_factor = 1;
        cinfo.comp_info[1].h_samp_factor = 1;
        cinfo.comp_info[1].v_samp_factor = 1;
        cinfo.comp_info[2].h_samp_factor = 1;
        cinfo.comp_info[2].v_samp_factor = 1;
    }

    jpeg_start_compress(&cinfo, TRUE);

    // EXIF must be the first APP marker after SOI. libjpeg's start_compress
    // emits JFIF (APP0) by default, but APP1 EXIF is the universally-respected
    // convention — write it now, immediately after start_compress.
    if (!exif.empty()) {
        jpeg_write_marker(&cinfo, kAppExif, exif.data(), static_cast<unsigned int>(exif.size()));
    }

    // ICC profile next (libjpeg splits into multiple APP2 segments as needed).
    if (!icc.empty()) {
        jpeg_write_icc_profile(&cinfo, icc.data(), static_cast<unsigned int>(icc.size()));
    }

    const size_t row_stride = static_cast<size_t>(width) * 3;
    while (cinfo.next_scanline < cinfo.image_height) {
        JSAMPROW rows[1] = {
            const_cast<JSAMPROW>(rgb + static_cast<size_t>(cinfo.next_scanline) * row_stride),
        };
        jpeg_write_scanlines(&cinfo, rows, 1);
    }

    jpeg_finish_compress(&cinfo);
    jpeg_destroy_compress(&cinfo);

    result.bytes.assign(out_buf, out_buf + out_len);
    free(out_buf);
    return result;
}

}  // namespace dsqueez
