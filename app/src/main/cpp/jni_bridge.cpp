#include "dsqueez.h"

#include <jni.h>
#include <android/log.h>

#define LOG_TAG "dsqueez-jni"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace {

void throw_desqueeze_exception(JNIEnv* env, const char* msg) {
    jclass cls = env->FindClass("app/dsqueez/nativebridge/DesqueezException");
    if (!cls) {
        // Fall back to RuntimeException if our class is missing (shouldn't happen).
        cls = env->FindClass("java/lang/RuntimeException");
    }
    if (cls) env->ThrowNew(cls, msg);
}

}  // namespace

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_app_dsqueez_nativebridge_Vips_desqueezeBytes(
    JNIEnv* env,
    jobject /* this */,
    jbyteArray j_src_bytes,
    jfloat j_ratio,
    jint j_out_format,
    jint j_quality
) {
    if (!j_src_bytes) {
        throw_desqueeze_exception(env, "srcBytes was null");
        return nullptr;
    }
    if (j_ratio <= 0.0f) {
        throw_desqueeze_exception(env, "ratio must be > 0");
        return nullptr;
    }

    jbyte* src = env->GetByteArrayElements(j_src_bytes, nullptr);
    if (!src) {
        throw_desqueeze_exception(env, "could not pin source bytes");
        return nullptr;
    }
    jsize src_len = env->GetArrayLength(j_src_bytes);

    const auto out_format = static_cast<dsqueez::OutFormat>(j_out_format);
    const int  quality    = static_cast<int>(j_quality);

    dsqueez::Result result = dsqueez::desqueeze_buffer(
        reinterpret_cast<uint8_t*>(src),
        static_cast<size_t>(src_len),
        static_cast<float>(j_ratio),
        out_format,
        quality
    );

    env->ReleaseByteArrayElements(j_src_bytes, src, JNI_ABORT);

    if (!result.error.empty() || !result.bytes) {
        const char* msg = result.error.empty() ? "unknown libvips error" : result.error.c_str();
        LOGE("desqueeze failed: %s", msg);
        if (result.bytes) dsqueez::free_result_bytes(result.bytes);
        throw_desqueeze_exception(env, msg);
        return nullptr;
    }

    jbyteArray out_array = env->NewByteArray(static_cast<jsize>(result.bytes_len));
    if (!out_array) {
        dsqueez::free_result_bytes(result.bytes);
        return nullptr;  // OOM — JVM will throw
    }
    env->SetByteArrayRegion(
        out_array,
        0,
        static_cast<jsize>(result.bytes_len),
        reinterpret_cast<const jbyte*>(result.bytes)
    );
    dsqueez::free_result_bytes(result.bytes);
    return out_array;
}
