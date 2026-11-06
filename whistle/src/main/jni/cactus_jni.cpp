#include <jni.h>
#include <android/log.h>

#include <cstdlib>

#include "cactus.h"

#define LOG_TAG "CactusWhistle"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace {
bool g_model_loaded = false;
constexpr int kMinOutputCapacity = 262144;
constexpr int kMaxOutputCapacity = 64 * 1024 * 1024;
constexpr int kOutputBytesPerSecond = 4096;

void throwJava(JNIEnv* env, const char* className, const char* message) {
    jclass exception = env->FindClass(className);
    if (exception != nullptr) {
        env->ThrowNew(exception, message);
    }
}
}  // namespace

extern "C" {

JNIEXPORT jint JNICALL
Java_dev_soupslurpr_transcribro_recognitionservice_whistle_CactusWhistleNative_nativeLoad(
        JNIEnv* env, jobject /*thiz*/, jbyteArray model) {
    if (g_model_loaded) {
        return 0;
    }
    if (model == nullptr) {
        throwJava(env, "java/lang/IllegalArgumentException", "model must not be null");
        return -1;
    }

    const jsize length = env->GetArrayLength(model);
    jbyte* bytes = env->GetByteArrayElements(model, nullptr);
    if (bytes == nullptr) {
        throwJava(env, "java/lang/IllegalStateException", "could not read model bytes");
        return -1;
    }

    const int result = cactus_load(
            reinterpret_cast<const unsigned char*>(bytes),
            static_cast<unsigned long long>(length));
    env->ReleaseByteArrayElements(model, bytes, JNI_ABORT);

    if (result < 0) {
        const char* error = cactus_last_error();
        LOGE("cactus_load failed: %s", error != nullptr ? error : "unknown error");
        throwJava(env, "java/lang/IllegalStateException",
                  error != nullptr ? error : "cactus_load failed");
        return result;
    }

    g_model_loaded = true;
    return result;
}

JNIEXPORT jstring JNICALL
Java_dev_soupslurpr_transcribro_recognitionservice_whistle_CactusWhistleNative_nativeTranscribe(
        JNIEnv* env, jobject /*thiz*/, jfloatArray pcm, jstring language,
        jboolean wordTimestamps) {
    if (!g_model_loaded) {
        throwJava(env, "java/lang/IllegalStateException", "Whistle model is not loaded");
        return nullptr;
    }
    if (pcm == nullptr) {
        throwJava(env, "java/lang/IllegalArgumentException", "pcm must not be null");
        return nullptr;
    }

    const jsize samples = env->GetArrayLength(pcm);
    jfloat* samplesData = env->GetFloatArrayElements(pcm, nullptr);
    if (samplesData == nullptr) {
        throwJava(env, "java/lang/IllegalStateException", "could not read pcm samples");
        return nullptr;
    }

    const char* languageChars = nullptr;
    if (language != nullptr) {
        languageChars = env->GetStringUTFChars(language, nullptr);
        if (languageChars == nullptr) {
            env->ReleaseFloatArrayElements(pcm, samplesData, JNI_ABORT);
            return nullptr;
        }
    }

    long long estimated = (static_cast<long long>(samples) / 16000) * kOutputBytesPerSecond;
    int outputCapacity = kMinOutputCapacity;
    if (estimated > outputCapacity) {
        outputCapacity = estimated > kMaxOutputCapacity ? kMaxOutputCapacity : static_cast<int>(estimated);
    }

    char* output = static_cast<char*>(std::calloc(outputCapacity, 1));
    if (output == nullptr) {
        if (languageChars != nullptr) {
            env->ReleaseStringUTFChars(language, languageChars);
        }
        env->ReleaseFloatArrayElements(pcm, samplesData, JNI_ABORT);
        throwJava(env, "java/lang/OutOfMemoryError", "could not allocate transcription buffer");
        return nullptr;
    }

    const int result = cactus_transcribe(
            samplesData,
            samples,
            languageChars,
            nullptr,
            wordTimestamps == JNI_TRUE ? 1 : 0,
            output,
            outputCapacity);

    output[outputCapacity - 1] = '\0';

    jstring transcript = nullptr;
    if (result < 0) {
        const char* error = cactus_last_error();
        LOGE("cactus_transcribe failed: %s", error != nullptr ? error : "unknown error");
        throwJava(env, "java/lang/IllegalStateException",
                  error != nullptr ? error : "cactus_transcribe failed");
    } else {
        transcript = env->NewStringUTF(output);
    }

    std::free(output);
    if (languageChars != nullptr) {
        env->ReleaseStringUTFChars(language, languageChars);
    }
    env->ReleaseFloatArrayElements(pcm, samplesData, JNI_ABORT);

    return transcript;
}

JNIEXPORT jstring JNICALL
Java_dev_soupslurpr_transcribro_recognitionservice_whistle_CactusWhistleNative_nativeLastError(
        JNIEnv* env, jobject /*thiz*/) {
    const char* error = cactus_last_error();
    return env->NewStringUTF(error != nullptr ? error : "");
}

}  // extern "C"
