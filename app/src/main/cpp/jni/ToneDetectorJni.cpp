#include "ToneDetector.h"

#include <jni.h>
#include <sstream>

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_willeypianotuning_toneanalyzer_ToneDetectorWrapper_createToneDetectorNative
        (JNIEnv *env, jobject) {
    auto *engine = new ToneDetector();

    return (jlong) engine;
}

JNIEXPORT void JNICALL
Java_com_willeypianotuning_toneanalyzer_ToneDetectorWrapper_destroyNativeClasses
        (JNIEnv *, jobject, jlong ptr) {
    auto *engine = (ToneDetector *) ptr;
    delete engine;
}

JNIEXPORT void JNICALL
Java_com_willeypianotuning_toneanalyzer_ToneDetectorWrapper_setRecalculateTuningNative
        (JNIEnv *env, jobject, jlong ptr, jboolean mode) {
    auto *engine = (ToneDetector *) ptr;
    if (engine != nullptr) {
        engine->setRecalculateTuning(mode);
    }
}

JNIEXPORT void JNICALL
Java_com_willeypianotuning_toneanalyzer_ToneDetectorWrapper_forceRecalculateNative
        (JNIEnv *env, jobject, jlong ptr) {
    auto *engine = (ToneDetector *) ptr;
    if (engine != nullptr) {
        engine->forceRecalculate();
    }
}

JNIEXPORT void JNICALL
Java_com_willeypianotuning_toneanalyzer_ToneDetectorWrapper_setNoteChangeModeNative
        (JNIEnv *env, jobject, jlong ptr, jint mode) {
    auto *engine = (ToneDetector *) ptr;
    if (engine != nullptr) {
        engine->setNoteChangeMode(mode);
    }
}

JNIEXPORT jint JNICALL
Java_com_willeypianotuning_toneanalyzer_ToneDetectorWrapper_getNoteChangeModeNative
        (JNIEnv *env, jobject, jlong ptr) {
    auto *engine = (ToneDetector *) ptr;
    if (engine != nullptr) {
        return engine->getNoteChangeMode();
    }
    return 0;
}

JNIEXPORT void JNICALL
Java_com_willeypianotuning_toneanalyzer_ToneDetectorWrapper_startPitchRaiseMeasurementNative
        (JNIEnv *env, jobject, jlong ptr, jintArray arr) {
    auto *engine = (ToneDetector *) ptr;
    if (engine != nullptr) {
        jint *body = env->GetIntArrayElements(arr, 0);
        engine->startPitchRaiseMeasurement(body);
        env->ReleaseIntArrayElements(arr, body, 0);
    }
}

JNIEXPORT void JNICALL
Java_com_willeypianotuning_toneanalyzer_ToneDetectorWrapper_stopPitchRaiseMeasurementNative
        (JNIEnv *env, jobject, jlong ptr) {
    auto *engine = (ToneDetector *) ptr;
    if (engine != nullptr) {
        engine->stopPitchRaiseMeasurement();
    }
}

JNIEXPORT jboolean JNICALL
Java_com_willeypianotuning_toneanalyzer_ToneDetectorWrapper_isPitchRaiseMeasurementOnNative
        (JNIEnv *env, jobject, jlong ptr) {
    auto *engine = (ToneDetector *) ptr;
    if (engine != nullptr) {
        return engine->isPitchRaiseMeasurementOn();
    }
    return false;
}

JNIEXPORT jdouble JNICALL
Java_com_willeypianotuning_toneanalyzer_ToneDetectorWrapper_getPitchOffsetFactorNative
        (JNIEnv *env, jobject, jlong ptr) {
    auto *engine = (ToneDetector *) ptr;
    if (engine != nullptr) {
        return engine->getPitchOffsetFactor();
    }
    return 1.0;
}

JNIEXPORT void JNICALL
Java_com_willeypianotuning_toneanalyzer_ToneDetectorWrapper_setPitchOffsetFactorNative
        (JNIEnv *env, jobject, jlong ptr, jdouble factor) {
    auto *engine = (ToneDetector *) ptr;
    if (engine != nullptr) {
        engine->setPitchOffsetFactor(factor);
    }
}

JNIEXPORT jdouble JNICALL
Java_com_willeypianotuning_toneanalyzer_ToneDetectorWrapper_getCalibrationFactorNative
        (JNIEnv *env, jobject, jlong ptr) {
    auto *engine = (ToneDetector *) ptr;
    if (engine != nullptr) {
        return engine->getCalibrationFactor();
    }

    return 1.0;
}

JNIEXPORT void JNICALL
Java_com_willeypianotuning_toneanalyzer_ToneDetectorWrapper_setCalibrationFactorNative
        (JNIEnv *env, jobject, jlong ptr, jdouble factor) {
    auto *engine = (ToneDetector *) ptr;
    if (engine != nullptr) {
        engine->setCalibrationFactor(factor);
    }
}

JNIEXPORT void JNICALL
Java_com_willeypianotuning_toneanalyzer_ToneDetectorWrapper_setInharmonicityWeightNative
        (JNIEnv *env, jobject, jlong ptr, jdouble inHarmonicityWeight) {
    auto *engine = (ToneDetector *) ptr;
    if (engine != nullptr) {
        engine->setInharmonicityWeight(inHarmonicityWeight);
    }
}

JNIEXPORT void JNICALL Java_com_willeypianotuning_toneanalyzer_ToneDetectorWrapper_addDataNative
        (JNIEnv *env, jobject, jlong ptr, jshortArray arr, jint bufferLen) {
    auto *engine = (ToneDetector *) ptr;
    if (engine != nullptr) {
        jshort *body = env->GetShortArrayElements(arr, nullptr);
        engine->addData(body, bufferLen);
        env->ReleaseShortArrayElements(arr, body, 0);
    }
}

JNIEXPORT jint JNICALL Java_com_willeypianotuning_toneanalyzer_ToneDetectorWrapper_getBxNative
        (JNIEnv *env, jobject, jlong ptr, jintArray arr) {
    auto *engine = (ToneDetector *) ptr;
    if (engine != nullptr) {
        jint *body = env->GetIntArrayElements(arr, nullptr);
        int len = engine->getBx(body);
        env->ReleaseIntArrayElements(arr, body, 0);
        return len;
    }
    return 0;
}

JNIEXPORT jint JNICALL Java_com_willeypianotuning_toneanalyzer_ToneDetectorWrapper_getByNative
        (JNIEnv *env, jobject, jlong ptr, jdoubleArray arr) {
    auto *engine = (ToneDetector *) ptr;
    if (engine != nullptr) {
        jdouble *body = env->GetDoubleArrayElements(arr, nullptr);
        int len = engine->getBy(body);
        env->ReleaseDoubleArrayElements(arr, body, 0);
        return len;
    }
    return 0;
}

JNIEXPORT jint JNICALL Java_com_willeypianotuning_toneanalyzer_ToneDetectorWrapper_getBxfitNative
        (JNIEnv *env, jobject, jlong ptr, jdoubleArray arr) {
    auto *engine = (ToneDetector *) ptr;
    if (engine != nullptr) {
        jdouble *body = env->GetDoubleArrayElements(arr, nullptr);
        int len = engine->getBxfit(body);
        env->ReleaseDoubleArrayElements(arr, body, 0);
        return len;
    }
    return 0;
}

JNIEXPORT jint JNICALL Java_com_willeypianotuning_toneanalyzer_ToneDetectorWrapper_getBaveNative
        (JNIEnv *env, jobject, jlong ptr, jdoubleArray arr) {
    auto *engine = (ToneDetector *) ptr;
    if (engine != nullptr) {
        jdouble *body = env->GetDoubleArrayElements(arr, nullptr);
        int len = engine->getBave(body);
        env->ReleaseDoubleArrayElements(arr, body, 0);
        return len;
    }
    return 0;
}

JNIEXPORT jint JNICALL Java_com_willeypianotuning_toneanalyzer_ToneDetectorWrapper_getFxNative
        (JNIEnv *env, jobject, jlong ptr, jdoubleArray arr) {
    auto *engine = (ToneDetector *) ptr;
    if (engine != nullptr) {
        jdouble *body = env->GetDoubleArrayElements(arr, nullptr);
        int len = engine->getFx(body);
        env->ReleaseDoubleArrayElements(arr, body, 0);
        return len;
    }
    return 0;
}

JNIEXPORT void JNICALL Java_com_willeypianotuning_toneanalyzer_ToneDetectorWrapper_setFxNative
        (JNIEnv *env, jobject, jlong ptr, jdoubleArray arr) {
    auto *engine = (ToneDetector *) ptr;
    if (engine != nullptr) {
        jdouble *body = env->GetDoubleArrayElements(arr, nullptr);
        engine->setFx(body);
        env->ReleaseDoubleArrayElements(arr, body, 0);
    }
}

JNIEXPORT jint JNICALL Java_com_willeypianotuning_toneanalyzer_ToneDetectorWrapper_getDeltaNative
        (JNIEnv *env, jobject, jlong ptr, jdoubleArray arr) {
    auto *engine = (ToneDetector *) ptr;
    if (engine != nullptr) {
        jdouble *body = env->GetDoubleArrayElements(arr, nullptr);
        int len = engine->getDelta(body);
        env->ReleaseDoubleArrayElements(arr, body, 0);
        return len;
    }
    return 0;
}

JNIEXPORT jint JNICALL
Java_com_willeypianotuning_toneanalyzer_ToneDetectorWrapper_getInharmonicityNative
        (JNIEnv *env, jobject, jlong ptr, jdoubleArray arr) {
    auto *engine = (ToneDetector *) ptr;
    if (engine != nullptr) {
        jdouble *body = env->GetDoubleArrayElements(arr, nullptr);
        int len = engine->getInharmonicity(body);
        env->ReleaseDoubleArrayElements(arr, body, 0);
        return len;
    }
    return 0;
}

JNIEXPORT void JNICALL
Java_com_willeypianotuning_toneanalyzer_ToneDetectorWrapper_setInputSamplingRateNative
        (JNIEnv *env, jobject, jlong ptr, jdouble sr) {
    auto *engine = (ToneDetector *) ptr;
    if (engine != nullptr) {
        engine->setInputSamplingRate(sr);
    }
}

JNIEXPORT jint JNICALL
Java_com_willeypianotuning_toneanalyzer_ToneDetectorWrapper_getPeaksHeightNative
        (JNIEnv *env, jobject, jlong ptr, jdoubleArray arr) {
    auto *engine = (ToneDetector *) ptr;
    if (engine != nullptr) {
        jdouble *body = env->GetDoubleArrayElements(arr, nullptr);
        int len = engine->getPeaksHeight(body);
        env->ReleaseDoubleArrayElements(arr, body, 0);
        return len;
    }
    return 0;
}

JNIEXPORT jint JNICALL
Java_com_willeypianotuning_toneanalyzer_ToneDetectorWrapper_getHarmonicsNative
        (JNIEnv *env, jobject, jlong ptr, jdoubleArray arr) {
    auto *engine = (ToneDetector *) ptr;
    if (engine != nullptr) {
        jdouble *body = env->GetDoubleArrayElements(arr, nullptr);
        int len = engine->getHarmonics(body);
        env->ReleaseDoubleArrayElements(arr, body, 0);
        return len;
    }
    return 0;
}

JNIEXPORT void JNICALL
Java_com_willeypianotuning_toneanalyzer_ToneDetectorWrapper_setHarmonicsNative
        (JNIEnv *env, jobject, jlong ptr, jdoubleArray arr) {
    auto *engine = (ToneDetector *) ptr;
    if (engine != nullptr) {
        jdouble *body = env->GetDoubleArrayElements(arr, nullptr);
        engine->setHarmonics(body);
        env->ReleaseDoubleArrayElements(arr, body, 0);
    }
}

JNIEXPORT void JNICALL Java_com_willeypianotuning_toneanalyzer_ToneDetectorWrapper_setDataNative
        (JNIEnv *env, jobject, jlong ptr,
         jdoubleArray peaksHeight,
         jdoubleArray inharmonicity,
         jdoubleArray delta,
         jdoubleArray bxfit,
         jdoubleArray fx,
         jdoubleArray harmonics) {
    auto *engine = (ToneDetector *) ptr;
    if (engine != nullptr) {
        engine->reset();
        jdouble *body;

        body = env->GetDoubleArrayElements(peaksHeight, nullptr);
        engine->setPeaksHeight(body);
        env->ReleaseDoubleArrayElements(peaksHeight, body, 0);

        body = env->GetDoubleArrayElements(inharmonicity, nullptr);
        engine->setInharmonicity(body);
        env->ReleaseDoubleArrayElements(inharmonicity, body, 0);

        /*body = env->GetDoubleArrayElements(delta, 0);
        engine->setDelta(body);
        env->ReleaseDoubleArrayElements(delta, body, 0);*/

        body = env->GetDoubleArrayElements(bxfit, nullptr);
        engine->setBxFit(body);
        env->ReleaseDoubleArrayElements(bxfit, body, 0);

        body = env->GetDoubleArrayElements(fx, nullptr);
        engine->setFx(body);
        env->ReleaseDoubleArrayElements(fx, body, 0);

        body = env->GetDoubleArrayElements(harmonics, nullptr);
        engine->setHarmonics(body);
        env->ReleaseDoubleArrayElements(harmonics, body, 0);

        engine->runTcCalculator(true);
    }
}

JNIEXPORT void JNICALL
Java_com_willeypianotuning_toneanalyzer_ToneDetectorWrapper_setInharmonicityNative
        (JNIEnv *env, jobject, jlong ptr,
         jdoubleArray inharmonicity) {
    auto *engine = (ToneDetector *) ptr;
    if (engine != nullptr) {
        jdouble *body = env->GetDoubleArrayElements(inharmonicity, nullptr);
        if (body != nullptr) {
            engine->setInharmonicity(body);
            env->ReleaseDoubleArrayElements(inharmonicity, body, JNI_ABORT);
        }

        engine->runTcCalculator(false);
    }
}

JNIEXPORT void JNICALL
Java_com_willeypianotuning_toneanalyzer_ToneDetectorWrapper_setTemperamentNative
        (JNIEnv *env, jobject, jlong ptr,
         jdoubleArray temperament) {
    auto *engine = (ToneDetector *) ptr;
    if (engine != nullptr) {
        jdouble *body = env->GetDoubleArrayElements(temperament, nullptr);
        if (body != nullptr) {
            engine->setTemperament(body);
            env->ReleaseDoubleArrayElements(temperament, body, JNI_ABORT);
        }

        engine->runTcCalculator(false);
    }
}

JNIEXPORT void JNICALL
Java_com_willeypianotuning_toneanalyzer_ToneDetectorWrapper_runTcCalculatorNative(
        JNIEnv *env, jobject, jlong ptr) {
    auto *engine = (ToneDetector *) ptr;
    if (engine != nullptr) {
        engine->runTcCalculator(true);
    }
}

JNIEXPORT void JNICALL
Java_com_willeypianotuning_toneanalyzer_ToneDetectorWrapper_setIntervalWeightsNative(JNIEnv *env,
                                                                                     jobject instance_,
                                                                                     jlong ptr,
                                                                                     jdoubleArray weights) {
    auto *engine = (ToneDetector *) ptr;
    if (engine != nullptr) {
        jdouble *body = env->GetDoubleArrayElements(weights, nullptr);
        if (body != nullptr) {
            engine->setWeights(body);
            env->ReleaseDoubleArrayElements(weights, body, JNI_ABORT);
        }
    }
}

JNIEXPORT jint JNICALL
Java_com_willeypianotuning_toneanalyzer_ToneDetectorWrapper_getDefaultIntervalWeightsNative(
        JNIEnv *env,
        jobject instance_,
        jlong ptr,
        jdoubleArray buffer) {
    jdouble *body = env->GetDoubleArrayElements(buffer, nullptr);
    if (body != nullptr) {
        IntervalWeights defaultWeights;
        unsigned int len = defaultWeights.get(body);
        env->ReleaseDoubleArrayElements(buffer, body, 0);
        return len;
    }
    return 0;
}

JNIEXPORT jint JNICALL
Java_com_willeypianotuning_toneanalyzer_ToneDetectorWrapper_getFFTResArrayNative
        (JNIEnv *env, jobject, jlong ptr, jfloatArray arr) {
    auto *engine = (ToneDetector *) ptr;
    if (engine != nullptr) {
        jfloat *body = env->GetFloatArrayElements(arr, nullptr);
        int len = engine->getFFTResArray(body);
        env->ReleaseFloatArrayElements(arr, body, 0);
        return len;
    }
    return 0;
}

JNIEXPORT jfloat JNICALL Java_com_willeypianotuning_toneanalyzer_ToneDetectorWrapper_getNumNative
        (JNIEnv *env, jobject, jlong ptr) {
    auto *engine = (ToneDetector *) ptr;
    if (engine != nullptr) {
        return engine->getNum();
    }
    return 0;
}

JNIEXPORT jbooleanArray JNICALL
Java_com_willeypianotuning_toneanalyzer_ToneDetectorWrapper_getSpinnerEnabledNative
        (JNIEnv *env, jobject, jlong ptr) {
    auto *engine = (ToneDetector *) ptr;
    if (engine != nullptr) {
        bool enabled[4];
        engine->getSpinnerEnabled(enabled);
        jbooleanArray jEnabledArray = env->NewBooleanArray(4);
        if (jEnabledArray == nullptr) {
            return nullptr;
        }
        jboolean jEnabled[4];
        for (int i = 0; i < 4; i++) {
            jEnabled[i] = enabled[i];
        }
        env->SetBooleanArrayRegion(jEnabledArray, 0, 4, jEnabled);
        return jEnabledArray;
    } else {
        return nullptr;
    }
}

JNIEXPORT jdoubleArray JNICALL
Java_com_willeypianotuning_toneanalyzer_ToneDetectorWrapper_getTargetPeakFrequenciesNative
        (JNIEnv *env, jobject, jlong ptr) {
    auto *engine = (ToneDetector *) ptr;
    if (engine != nullptr) {
        double freq[5];
        int n = engine->getTargetPeakFreq(freq);
        jdoubleArray jOutput = env->NewDoubleArray(n);
        if (jOutput == nullptr) {
            return nullptr;
        }
        env->SetDoubleArrayRegion(jOutput, 0, n, freq);

        return jOutput;
    } else {
        return nullptr;
    }
}

JNIEXPORT jfloat JNICALL Java_com_willeypianotuning_toneanalyzer_ToneDetectorWrapper_getAngleNative
        (JNIEnv *env, jobject, jlong ptr) {
    auto *engine = (ToneDetector *) ptr;
    if (engine != nullptr) {
        return engine->getAngle();
    }
    return 0;
}

JNIEXPORT jfloat JNICALL
Java_com_willeypianotuning_toneanalyzer_ToneDetectorWrapper_getCentsOffsetAvgNative
        (JNIEnv *env, jobject, jlong ptr) {
    auto *engine = (ToneDetector *) ptr;
    if (engine != nullptr) {
        return engine->getCentsOffsetAvg();
    }
    return 0;
}

JNIEXPORT jfloat JNICALL
Java_com_willeypianotuning_toneanalyzer_ToneDetectorWrapper_getCentsOffsetZCAvgNative
        (JNIEnv *env, jobject, jlong ptr) {
    auto *engine = (ToneDetector *) ptr;
    if (engine != nullptr) {
        return engine->getCentsOffsetZCAvg();
    }
    return 0;
}

JNIEXPORT jfloat JNICALL
Java_com_willeypianotuning_toneanalyzer_ToneDetectorWrapper_getCentsOffsetCombinedNative
        (JNIEnv *env, jobject, jlong ptr) {
    auto *engine = (ToneDetector *) ptr;
    if (engine != nullptr) {
        return engine->getCentsOffsetCombined();
    }
    return 0;
}

JNIEXPORT jint JNICALL
Java_com_willeypianotuning_toneanalyzer_ToneDetectorWrapper_getCurrentNoteNative
        (JNIEnv *env, jobject, jlong ptr) {
    auto *engine = (ToneDetector *) ptr;
    if (engine != nullptr) {
        return engine->getCurrentNote();
    }
    return 0;
}

JNIEXPORT jint JNICALL
Java_com_willeypianotuning_toneanalyzer_ToneDetectorWrapper_getNSCNative
        (JNIEnv *env, jobject, jlong ptr) {
    auto *engine = (ToneDetector *) ptr;
    if (engine != nullptr) {
        return engine->getNSC();
    }
    return 0;
}

JNIEXPORT jint JNICALL
Java_com_willeypianotuning_toneanalyzer_ToneDetectorWrapper_getCandidateNoteNative
        (JNIEnv *env, jobject, jlong ptr) {
    auto *engine = (ToneDetector *) ptr;
    if (engine != nullptr) {
        return engine->getCandidateNote();
    }
    return 0;
}

JNIEXPORT jint JNICALL Java_com_willeypianotuning_toneanalyzer_ToneDetectorWrapper_getAcToneNative
        (JNIEnv *env, jobject, jlong ptr) {
    auto *engine = (ToneDetector *) ptr;
    if (engine != nullptr) {
        return engine->getAcTone();
    }
    return 0;
}

JNIEXPORT jboolean JNICALL
Java_com_willeypianotuning_toneanalyzer_ToneDetectorWrapper_getOffsetOverNative
        (JNIEnv *env, jobject, jlong ptr) {
    auto *engine = (ToneDetector *) ptr;
    if (engine != nullptr) {
        return engine->isOffsetOver();
    }
    return false;
}

JNIEXPORT jfloatArray JNICALL
Java_com_willeypianotuning_toneanalyzer_ToneDetectorWrapper_getPhaseAndAlphaNative
        (JNIEnv *env, jobject, jlong ptr, jint partial) {
    auto *engine = (ToneDetector *) ptr;
    if (engine != nullptr) {
        std::vector<float> ring_state;
        engine->getPhase(partial, &ring_state);
        jfloatArray jOutput = env->NewFloatArray(ring_state.size());
        if (jOutput != nullptr) {
            env->SetFloatArrayRegion(jOutput, 0, ring_state.size(), ring_state.data());
        }
        return jOutput;
    }
    return nullptr;
}

JNIEXPORT void JNICALL Java_com_willeypianotuning_toneanalyzer_ToneDetectorWrapper_skipPhasesNative
        (JNIEnv *env, jobject, jlong ptr, jint partial, jint skip) {
    auto *engine = (ToneDetector *) ptr;
    if (engine != nullptr) {
        engine->skipPhases(partial, skip);
    }
}

JNIEXPORT jboolean JNICALL
Java_com_willeypianotuning_toneanalyzer_ToneDetectorWrapper_isQualityTestOkNative
        (JNIEnv *env, jobject, jlong ptr) {
    auto *engine = (ToneDetector *) ptr;
    if (engine != nullptr) {
        return engine->isQualityTestOk();
    }
    return false;
}

JNIEXPORT jboolean JNICALL
Java_com_willeypianotuning_toneanalyzer_ToneDetectorWrapper_isTargetLengthCountedNative
        (JNIEnv *env, jobject, jlong ptr) {
    auto *engine = (ToneDetector *) ptr;
    if (engine != nullptr) {
        return engine->isTargetLengthCounted();
    }
    return false;
}

JNIEXPORT void JNICALL
Java_com_willeypianotuning_toneanalyzer_ToneDetectorWrapper_setCurrentNoteNative
        (JNIEnv *env, jobject, jlong ptr, jint note, jboolean relative) {
    auto *engine = (ToneDetector *) ptr;
    if (engine != nullptr) {
        engine->setCurrentNote(note, relative, true);
    }
}

JNIEXPORT void JNICALL
Java_com_willeypianotuning_toneanalyzer_ToneDetectorWrapper_setNSCNative
        (JNIEnv *env, jobject, jlong ptr, jint value) {
    auto *engine = (ToneDetector *) ptr;
    if (engine != nullptr) {
        engine->setNSC(value);
    }
}

JNIEXPORT jboolean JNICALL
Java_com_willeypianotuning_toneanalyzer_ToneDetectorWrapper_processFrameNative
        (JNIEnv *env, jobject, jlong ptr) {
    auto *engine = (ToneDetector *) ptr;
    if (engine != nullptr) {
        return engine->fastLoop();
    }
    return false;
}

JNIEXPORT jboolean JNICALL
Java_com_willeypianotuning_toneanalyzer_ToneDetectorWrapper_detectNotesNative
        (JNIEnv *env, jobject, jlong ptr) {
    auto *engine = (ToneDetector *) ptr;
    if (engine != nullptr) {
        return engine->detectNotes();
    }
    return false;
}

JNIEXPORT void JNICALL
Java_com_willeypianotuning_toneanalyzer_ToneDetectorWrapper_processZeroCrossingNative
        (JNIEnv *env, jobject, jlong ptr) {
    auto *engine = (ToneDetector *) ptr;
    if (engine != nullptr) {
        engine->processZeroCrossing();
    }
}

JNIEXPORT void JNICALL Java_com_willeypianotuning_toneanalyzer_ToneDetectorWrapper_resetNative
        (JNIEnv *env, jobject, jlong ptr) {
    auto *engine = (ToneDetector *) ptr;
    if (engine != nullptr) {
        engine->reset();
    }
}

JNIEXPORT void JNICALL
Java_com_willeypianotuning_toneanalyzer_ToneDetectorWrapper_setOverpullCentsNative(JNIEnv *env,
                                                                                   jobject thiz,
                                                                                   jlong instance,
                                                                                   jdoubleArray buffer) {
    auto *engine = (ToneDetector *) instance;
    if (engine != nullptr) {
        jdouble *samples = env->GetDoubleArrayElements(buffer, nullptr);
        if (samples != nullptr) {
            engine->setOverpullCents(samples);
            env->ReleaseDoubleArrayElements(buffer, samples, JNI_ABORT);
        }
    }
}

JNIEXPORT jdoubleArray JNICALL
Java_com_willeypianotuning_toneanalyzer_ToneDetectorWrapper_getIntervalWidthsNative(JNIEnv *env,
                                                                                    jobject thiz,
                                                                                    jlong instance,
                                                                                    jboolean use_cents) {
    auto *engine = (ToneDetector *) instance;
    if (engine != nullptr) {
        jdoubleArray jOutput = env->NewDoubleArray(32 * 88);
        if (jOutput != nullptr) {
            double widths[32 * 88]{0};
            engine->getIntervalWidths(widths, use_cents);
            env->SetDoubleArrayRegion(jOutput, 0, 32 * 88, widths);
        }
        return jOutput;
    }
    return nullptr;
}

JNIEXPORT jlong JNICALL
Java_com_willeypianotuning_toneanalyzer_ToneDetectorWrapper_getPianoKeyFrequenciesPtr(JNIEnv *env,
                                                                                      jobject thiz,
                                                                                      jlong instance) {
    auto *engine = (ToneDetector *) instance;
    if (engine != nullptr) {
        return (jlong) engine->getPianoKeyFrequenciesPtr();
    }
    return 0;
}
}