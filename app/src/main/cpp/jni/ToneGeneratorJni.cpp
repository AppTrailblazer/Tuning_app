#include "ToneGenerator.h"
#include "PianoKeyFrequencies.h"

#include <jni.h>
#include <sstream>

extern "C" {
JNIEXPORT jlong JNICALL
Java_com_willeypianotuning_toneanalyzer_generator_ToneGeneratorWrapper_createNative
        (JNIEnv *env, jobject thiz, jlong pianoKeyFrequenciesPtr) {
    auto *pianoKeyFrequencies = (PianoKeyFrequencies *) pianoKeyFrequenciesPtr;
    if (pianoKeyFrequencies == nullptr) {
        pianoKeyFrequencies = new PianoKeyFrequencies();
    }
    auto *engine = new ToneGenerator(pianoKeyFrequencies);
    return (jlong) engine;
}

JNIEXPORT void JNICALL
Java_com_willeypianotuning_toneanalyzer_generator_ToneGeneratorWrapper_destroyNative
        (JNIEnv *env, jobject, jlong ptr) {
    auto *engine = (ToneGenerator *) ptr;
    delete engine;
}

JNIEXPORT void JNICALL
Java_com_willeypianotuning_toneanalyzer_generator_ToneGeneratorWrapper_setAudioFrequency(
        JNIEnv *env, jobject thiz, jlong ptr, jint frequency) {
    auto *engine = (ToneGenerator *) ptr;
    if (engine != nullptr) {
        engine->setAudioFrequency(frequency);
    }
}

JNIEXPORT void JNICALL
Java_com_willeypianotuning_toneanalyzer_generator_ToneGeneratorWrapper_setTrebleBassOptions(
        JNIEnv *env, jobject thiz, jlong ptr, jfloat treble_volume, jshort treble_edge,
        jfloat bass_volume, jshort bass_edge) {
    auto *engine = (ToneGenerator *) ptr;
    if (engine != nullptr) {
        engine->setTrebleBassOptions(treble_volume, treble_edge, bass_volume, bass_edge);
    }
}

JNIEXPORT void JNICALL
Java_com_willeypianotuning_toneanalyzer_generator_ToneGeneratorWrapper_initTone(JNIEnv *env,
                                                                                jobject thiz,
                                                                                jlong ptr,
                                                                                jint noteZeroIndexed) {
    auto *engine = (ToneGenerator *) ptr;
    if (engine != nullptr) {
        engine->initTone(noteZeroIndexed);
    }
}

JNIEXPORT void JNICALL
Java_com_willeypianotuning_toneanalyzer_generator_ToneGeneratorWrapper_generateTone(JNIEnv *env,
                                                                                    jobject thiz,
                                                                                    jlong ptr,
                                                                                    jshortArray buffer,
                                                                                    jint bufferLen) {
    auto *engine = (ToneGenerator *) ptr;
    if (engine != nullptr) {
        jshort *body = env->GetShortArrayElements(buffer, nullptr);
        engine->generateTone(body, bufferLen);
        env->ReleaseShortArrayElements(buffer, body, 0);
    }
}

JNIEXPORT void JNICALL
Java_com_willeypianotuning_toneanalyzer_generator_ToneGeneratorWrapper_getVolumeMultiplier(
        JNIEnv *env, jobject thiz, jlong ptr, jfloatArray buffer) {
    auto *engine = (ToneGenerator *) ptr;
    if (engine != nullptr) {
        jfloat *body = env->GetFloatArrayElements(buffer, nullptr);
        engine->getVolumeMultiplier(body);
        env->ReleaseFloatArrayElements(buffer, body, 0);
    }
}

JNIEXPORT void JNICALL
Java_com_willeypianotuning_toneanalyzer_generator_ToneGeneratorWrapper_getPlaybackPartials(
        JNIEnv *env, jobject thiz, jlong ptr, jshortArray buffer) {
    auto *engine = (ToneGenerator *) ptr;
    if (engine != nullptr) {
        jshort *body = env->GetShortArrayElements(buffer, nullptr);
        engine->getPlaybackPartials(body);
        env->ReleaseShortArrayElements(buffer, body, 0);
    }
}

JNIEXPORT void JNICALL
Java_com_willeypianotuning_toneanalyzer_generator_ToneGeneratorWrapper_getHarmonicsAmplitudes(
        JNIEnv *env, jobject thiz, jlong ptr, jfloatArray buffer) {
    auto *engine = (ToneGenerator *) ptr;
    if (engine != nullptr) {
        jfloat *body = env->GetFloatArrayElements(buffer, nullptr);
        engine->getHarmonicsAmplitudes(body);
        env->ReleaseFloatArrayElements(buffer, body, 0);
    }
}
}