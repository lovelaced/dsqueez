// Host-side smoke test for the dsqueez pipeline.
//
// Compile:
//   clang++ -std=c++17 -O2 -I/opt/homebrew/include \
//     -I<repo>/app/src/main/cpp \
//     /tmp/dsq_test.cpp \
//     <repo>/app/src/main/cpp/lanczos.cpp \
//     <repo>/app/src/main/cpp/jpeg_pipeline.cpp \
//     <repo>/app/src/main/cpp/dsqueez.cpp \
//     -L/opt/homebrew/lib -ljpeg \
//     -o /tmp/dsq_test
//
// Run:
//   /tmp/dsq_test input.jpg output.jpg [ratio] [orientation]

#include "dsqueez.h"

#include <cstdio>
#include <cstdlib>
#include <vector>
#include <fstream>
#include <sstream>

namespace {
std::vector<uint8_t> read_file(const char* path) {
    std::ifstream f(path, std::ios::binary);
    std::ostringstream ss;
    ss << f.rdbuf();
    const std::string s = ss.str();
    return {s.begin(), s.end()};
}

void write_file(const char* path, const std::vector<uint8_t>& bytes) {
    std::ofstream f(path, std::ios::binary);
    f.write(reinterpret_cast<const char*>(bytes.data()), bytes.size());
}
}  // namespace

// Stub the Android logger so the C++ pipeline links cleanly on the host.
extern "C" int __android_log_print(int /*prio*/, const char* tag, const char* fmt, ...) {
    va_list ap;
    va_start(ap, fmt);
    std::fprintf(stderr, "[%s] ", tag);
    std::vfprintf(stderr, fmt, ap);
    std::fputc('\n', stderr);
    va_end(ap);
    return 0;
}

int main(int argc, char** argv) {
    if (argc < 3) {
        std::fprintf(stderr, "usage: %s input.jpg output.jpg [ratio=1.33] [orientation=1]\n", argv[0]);
        return 1;
    }
    const char* in_path  = argv[1];
    const char* out_path = argv[2];
    const float ratio       = (argc > 3) ? std::strtof(argv[3], nullptr) : 1.33f;
    const int   orientation = (argc > 4) ? std::atoi(argv[4]) : 1;

    auto bytes = read_file(in_path);
    if (bytes.empty()) {
        std::fprintf(stderr, "could not read %s\n", in_path);
        return 1;
    }
    std::fprintf(stderr, "input %s: %zu bytes\n", in_path, bytes.size());

    dsqueez::Result r = dsqueez::desqueeze_jpeg(
        bytes.data(), bytes.size(), ratio, 95, orientation
    );
    if (!r.error.empty()) {
        std::fprintf(stderr, "FAILED: %s\n", r.error.c_str());
        return 2;
    }
    write_file(out_path, r.bytes);
    std::fprintf(stderr, "wrote %s: %zu bytes\n", out_path, r.bytes.size());
    return 0;
}
