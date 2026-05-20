cmake_minimum_required(VERSION 3.22.1)
project(dsqueez LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Prebuilt libvips (and transitive deps) live under src/main/jniLibs/${ANDROID_ABI}/.
# We declare them as IMPORTED targets so CMake can locate them at build time.
set(JNILIBS_DIR "${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}")
set(VIPS_HEADERS_DIR "${CMAKE_SOURCE_DIR}/vips_headers")

# IMPORTED targets for each .so we expect to vendor.
# Names match the file basenames libvips builds produce on Android.
foreach(LIB vips gobject-2.0 glib-2.0 gio-2.0 expat z heif jpeg)
    add_library(${LIB} SHARED IMPORTED)
    set_target_properties(${LIB} PROPERTIES
        IMPORTED_LOCATION "${JNILIBS_DIR}/lib${LIB}.so"
    )
endforeach()

# Our JNI bridge — small wrapper around vips_affine + vips_xxxsave_buffer.
add_library(dsqueez SHARED
    jni_bridge.cpp
    dsqueez.cpp
)

target_include_directories(dsqueez PRIVATE
    ${VIPS_HEADERS_DIR}
    ${VIPS_HEADERS_DIR}/glib-2.0
    ${VIPS_HEADERS_DIR}/vips
)

find_library(log-lib log)
find_library(android-lib android)

target_link_libraries(dsqueez
    vips gobject-2.0 glib-2.0 gio-2.0 expat z heif jpeg
    ${log-lib} ${android-lib}
)
