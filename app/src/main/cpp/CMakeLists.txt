cmake_minimum_required(VERSION 3.22.1)
project(dsqueez LANGUAGES C CXX)

set(CMAKE_C_STANDARD 11)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

# -----------------------------------------------------------------------------
# libjpeg-turbo via FetchContent. Pinned tag, downloaded + cached on first
# configure, built statically and linked into libdsqueez.so. No separately
# vendored .so files to ship — one self-contained native artifact.
#
# Note: upstream libjpeg-turbo's CMakeLists FATAL_ERRORs on add_subdirectory(),
# which is what FetchContent_MakeAvailable() does under the hood. We patch the
# guard portably (CMake file ops, no shell sed) and then add_subdirectory
# ourselves. The patch is applied once at populate time and persists in the
# FetchContent cache.
# -----------------------------------------------------------------------------
include(FetchContent)

FetchContent_Declare(
    libjpeg_turbo
    URL      https://github.com/libjpeg-turbo/libjpeg-turbo/releases/download/3.1.4.1/libjpeg-turbo-3.1.4.1.tar.gz
    URL_HASH SHA256=ecae8008e2cc9ade2f2c1bb9d5e6d4fb73e7c433866a056bd82980741571a022
)

FetchContent_GetProperties(libjpeg_turbo)
if(NOT libjpeg_turbo_POPULATED)
    FetchContent_Populate(libjpeg_turbo)

    set(_ljt_cmake "${libjpeg_turbo_SOURCE_DIR}/CMakeLists.txt")
    file(READ "${_ljt_cmake}" _ljt_content)
    string(REPLACE
        "message(FATAL_ERROR \"The"
        "message(STATUS \"[dsqueez-patched] The"
        _ljt_content "${_ljt_content}")
    file(WRITE "${_ljt_cmake}" "${_ljt_content}")
endif()

set(ENABLE_SHARED  OFF CACHE BOOL "" FORCE)
set(ENABLE_STATIC  ON  CACHE BOOL "" FORCE)
set(WITH_TURBOJPEG OFF CACHE BOOL "" FORCE)
set(WITH_JAVA      OFF CACHE BOOL "" FORCE)
set(WITH_JPEG8     OFF CACHE BOOL "" FORCE)
set(WITH_SIMD      ON  CACHE BOOL "" FORCE)

add_subdirectory("${libjpeg_turbo_SOURCE_DIR}" "${libjpeg_turbo_BINARY_DIR}" EXCLUDE_FROM_ALL)

# -----------------------------------------------------------------------------
# Our native pipeline. JPEG-in, JPEG-out, custom Lanczos-3 against libjpeg-turbo.
# -----------------------------------------------------------------------------
add_library(dsqueez SHARED
    jni_bridge.cpp
    dsqueez.cpp
    lanczos.cpp
    jpeg_pipeline.cpp
)

target_include_directories(dsqueez PRIVATE
    "${libjpeg_turbo_SOURCE_DIR}"
    "${libjpeg_turbo_SOURCE_DIR}/src"
    "${libjpeg_turbo_BINARY_DIR}"    # for jconfig.h / jconfigint.h
)

target_compile_options(dsqueez PRIVATE
    -Wall -Wextra -O3 -fvisibility=hidden
)

find_library(log-lib log)

target_link_libraries(dsqueez
    jpeg-static
    ${log-lib}
)
