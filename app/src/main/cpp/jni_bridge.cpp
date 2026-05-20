// JNI bridge — only built on Android. Host smoke tests link dsqueez.cpp +
// lanczos.cpp + jpeg_pipeline.cpp directly without this file.

#if defined(__ANDROID__)

#include "dsqueez.h"

#include <jni.h>
#include <android/log.h>

#define LOG_TAG "dsqueez-jni"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace {

void throw_dsqueez_exception(JNIEnv* env, const char* msg) {
    jclass cls = env->FindClass("app/dsqueez/nativebridge/ResamplerException");
    if (!cls) cls = env->FindClass("java/lang/RuntimeException");
    if (cls) env->ThrowNew(cls, msg);
}

}  // namespace

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_app_dsqueez_nativebridge_Resampler_desqueezeBytes(
    JNIEnv*    env,
    jobject /* this */,
    jbyteArray j_src_bytes,
    jfloat     j_ratio,
    jint       j_quality,
    jint       j_orientation
) {
    if (!j_src_bytes) {
        throw_dsqueez_exception(env, "srcBytes was null");
        return nullptr;
    }
    if (j_ratio <= 0.0f) {
        throw_dsqueez_exception(env, "ratio must be > 0");
        return nullptr;
    }

    jbyte* src = env->GetByteArrayElements(j_src_bytes, nullptr);
    if (!src) {
        throw_dsqueez_exception(env, "could not pin source bytes");
        return nullptr;
    }
    const jsize src_len = env->GetArrayLength(j_src_bytes);

    dsqueez::Result result = dsqueez::desqueeze_jpeg(
        reinterpret_cast<uint8_t*>(src),
        static_cast<size_t>(src_len),
        static_cast<float>(j_ratio),
        static_cast<int>(j_quality),
        static_cast<int>(j_orientation)
    );

    env->ReleaseByteArrayElements(j_src_bytes, src, JNI_ABORT);

    if (!result.error.empty()) {
        LOGE("desqueeze failed: %s", result.error.c_str());
        throw_dsqueez_exception(env, result.error.c_str());
        return nullptr;
    }

    jbyteArray out_array = env->NewByteArray(static_cast<jsize>(result.bytes.size()));
    if (!out_array) return nullptr;
    env->SetByteArrayRegion(
        out_array, 0, static_cast<jsize>(result.bytes.size()),
        reinterpret_cast<const jbyte*>(result.bytes.data())
    );
    return out_array;
}


#endif  // __ANDROID__
