#include "PianoKeyFrequencies.h"
#include "ToneDetectorConstants.h"
#include <cmath>
#include "debug_util.h"

PianoKeyFrequencies::PianoKeyFrequencies() {
    computeFrequencies();
}

void PianoKeyFrequencies::computeFrequencies() {
    for (int i = 0; i < NOTES_ON_PIANO; i++) {
        PIANO_KEY_FREQUENCIES[i] = powf(2, ((i - 48) / 12.0F)) * standardFrequency;
        PIANO_KEY_FREQUENCIES_P12[i] = powf(3, ((i - 48) / 19.0F)) * standardFrequency;
    }
}

void PianoKeyFrequencies::setOffsetFactor(double factor) {
    pitchOffsetFactor = factor;
    standardFrequency = STANDARD_FREQUENCY * factor;
    computeFrequencies();
}

/**
 * @return note key (1-indexed)
 */
int PianoKeyFrequencies::closestKey(double freq, const double *keyFrequencies) {
    int index;
    int minIndex = 0;
    int maxIndex = NOTES_ON_PIANO - 1;
    if (freq < keyFrequencies[minIndex]) {
        index = minIndex;
    } else if (freq > keyFrequencies[maxIndex]) {
        index = maxIndex;
    } else {
        int newIndex;
        while (maxIndex - minIndex > 1) {
            newIndex = (int) ((maxIndex + minIndex) / 2.0);
            if (freq < keyFrequencies[newIndex]) {
                maxIndex = newIndex;
            } else {
                minIndex = newIndex;
            }
        }
        if (fabs(freq - keyFrequencies[minIndex]) <
            fabs(freq - keyFrequencies[maxIndex])) {
            index = minIndex;
        } else {
            index = maxIndex;
        }
    }
    return index + 1;
}

int PianoKeyFrequencies::closestKey(double freq) {
    return closestKey(freq, PIANO_KEY_FREQUENCIES);
}

int PianoKeyFrequencies::closestKeyP12(double freq) {
    return closestKey(freq, PIANO_KEY_FREQUENCIES_P12);
}

double PianoKeyFrequencies::frequency(int noteZeroIndexed) {
    ASSERT_RANGE(noteZeroIndexed, 0, NOTES_ON_PIANO - 1);
    return PIANO_KEY_FREQUENCIES[noteZeroIndexed];
}

double PianoKeyFrequencies::frequencyP12(int noteZeroIndexed) {
    ASSERT_RANGE(noteZeroIndexed, 0, NOTES_ON_PIANO - 1);
    return PIANO_KEY_FREQUENCIES_P12[noteZeroIndexed];
}

double PianoKeyFrequencies::getOffsetFactor() {
    return pitchOffsetFactor;
}

void PianoKeyFrequencies::computeTargetFrequencies(const double *delta, const double *Bave) {
    for (int n = 0; n < 10; n++) {
        int nn = n + 1;
        for (int noteZeroIndexed = 0; noteZeroIndexed < NOTES_ON_PIANO; noteZeroIndexed++) {
            double v1 = pow(2, ((delta[noteZeroIndexed]) / 1200));
            double v2 = (1 + nn * nn * Bave[noteZeroIndexed]) / (1 + Bave[noteZeroIndexed]);
            targetFreq[n][noteZeroIndexed] = frequency(noteZeroIndexed) * nn * v1 * sqrt(v2);
        }
    }
}

double PianoKeyFrequencies::targetFrequency(int partial, int noteZeroIndexed) {
    ASSERT_RANGE(partial, 0, 9);
    ASSERT_RANGE(noteZeroIndexed, 0, NOTES_ON_PIANO - 1);
    return targetFreq[partial][noteZeroIndexed];
}
