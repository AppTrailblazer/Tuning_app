#include "Spinner.h"

#include <jni.h>
#include <sstream>

extern "C" {
JNIEXPORT jlong JNICALL Java_com_willeypianotuning_toneanalyzer_spinners_Spinner_allocateNative
        (JNIEnv *env, jobject, jint id, jint sampleRate, jint samplesPerUpdate,
         jdouble bpfBandwidth) {
    auto *spinner = new Spinner(id, sampleRate, samplesPerUpdate, bpfBandwidth);
    return (jlong) spinner;
}

JNIEXPORT void JNICALL Java_com_willeypianotuning_toneanalyzer_spinners_Spinner_freeNative
        (JNIEnv *env, jobject, jlong _spinner) {
    auto *spinner = (Spinner *) _spinner;
    delete spinner;
}

JNIEXPORT void JNICALL Java_com_willeypianotuning_toneanalyzer_spinners_Spinner_setFrequencyNative
        (JNIEnv *env, jobject, jlong _spinner, jdouble freq) {
    auto *spinner = (Spinner *) _spinner;
    if (spinner != nullptr) {
        spinner->setFrequency(freq);
    }
}

JNIEXPORT void JNICALL
Java_com_willeypianotuning_toneanalyzer_spinners_Spinner_setDetectionParamNative
        (JNIEnv *env, jobject, jlong _spinner, jdouble threshold) {
    auto *spinner = (Spinner *) _spinner;
    if (spinner != nullptr) {
        spinner->setDetectionParam(threshold);
    }
}

JNIEXPORT jfloatArray JNICALL
Java_com_willeypianotuning_toneanalyzer_spinners_Spinner_getPhaseNative
        (JNIEnv *env, jobject, jlong _spinner, jshortArray jSamples) {
    auto *spinner = (Spinner *) _spinner;
    if (spinner != nullptr) {
        jshort *samples = env->GetShortArrayElements(jSamples, nullptr);
        if (samples != nullptr) {
            int numSamples = env->GetArrayLength(jSamples);
            std::vector<float> phase = spinner->getPhase(samples, numSamples);
            env->ReleaseShortArrayElements(jSamples, samples, 0);

            jfloatArray jOutput = env->NewFloatArray(phase.size());
            if (jOutput != nullptr) {
                env->SetFloatArrayRegion(jOutput, 0, phase.size(), phase.data());
            }
            return jOutput;
        }
    }
    return nullptr;
}
}