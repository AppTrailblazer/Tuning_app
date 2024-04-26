#include "IntervalWeights.h"
#include "ToneDetectorConstants.h"
#include "debug_util.h"
#include <stdio.h>
#include <string>

IntervalWeights::IntervalWeights() {

}

void IntervalWeights::set(const double *weights) {
    int position = 0;
    for (int i = 0; i < OCTAVE_WEIGHTS_LEN; ++i) {
        octaveWeights[i] = weights[position + i];
    }
    position += OCTAVE_WEIGHTS_LEN;

    for (int i = 0; i < TWELFTH_WEIGHTS_LEN; ++i) {
        twelfthWeights[i] = weights[position + i];
    }
    position += TWELFTH_WEIGHTS_LEN;

    for (int i = 0; i < DOUBLE_OCTAVE_WEIGHTS_LEN; ++i) {
        doubleOctaveWeights[i] = weights[position + i];
    }
    position += DOUBLE_OCTAVE_WEIGHTS_LEN;

    for (int i = 0; i < NINETEENTH_WEIGHTS_LEN; ++i) {
        nineteenthWeights[i] = weights[position + i];
    }
    position += NINETEENTH_WEIGHTS_LEN;

    for (int i = 0; i < TRIPLE_OCTAVE_WEIGHTS_LEN; ++i) {
        tripleOctaveWeights[i] = weights[position + i];
    }
    position += TRIPLE_OCTAVE_WEIGHTS_LEN;

    for (int i = 0; i < FIFTH_WEIGHTS_LEN; ++i) {
        fifthWeights[i] = weights[position + i];
    }
    position += FIFTH_WEIGHTS_LEN;

    for (int i = 0; i < FOURTH_WEIGHTS_LEN; ++i) {
        fourthWeights[i] = weights[position + i];
    }
    position += FOURTH_WEIGHTS_LEN;
    centsPerOctaveTreble = weights[position++];
    if (centsPerOctaveTreble < EXTRA_TREBLE_STRETCH_CENTS_PER_OCTAVE_MIN ||
        centsPerOctaveTreble > EXTRA_TREBLE_STRETCH_CENTS_PER_OCTAVE_MAX) {
        LOGD("ExtraTrebleCentsPerOctave %.2f is out of range (%.2f:%.2f). Normalizing.",
             centsPerOctaveTreble, EXTRA_TREBLE_STRETCH_CENTS_PER_OCTAVE_MIN,
             EXTRA_TREBLE_STRETCH_CENTS_PER_OCTAVE_MAX);
        centsPerOctaveTreble = std::max(
                std::min(centsPerOctaveTreble, EXTRA_TREBLE_STRETCH_CENTS_PER_OCTAVE_MAX),
                EXTRA_TREBLE_STRETCH_CENTS_PER_OCTAVE_MIN);
    }
    notesToStretchTreble = (int) weights[position++];
    if (notesToStretchTreble < EXTRA_TREBLE_STRETCH_OCTAVES_MIN ||
        notesToStretchTreble > EXTRA_TREBLE_STRETCH_OCTAVES_MAX) {
        LOGD("ExtraTrebleOctaves %d is out of range (%d:%d). Normalizing.", notesToStretchTreble,
             EXTRA_TREBLE_STRETCH_OCTAVES_MIN, EXTRA_TREBLE_STRETCH_OCTAVES_MAX);
        notesToStretchTreble = std::max(
                std::min(notesToStretchTreble, EXTRA_TREBLE_STRETCH_OCTAVES_MAX),
                EXTRA_TREBLE_STRETCH_OCTAVES_MIN);
    }
    centsPerOctaveBass = weights[position++];
    if (centsPerOctaveBass < EXTRA_BASS_STRETCH_CENTS_PER_OCTAVE_MIN ||
        centsPerOctaveBass > EXTRA_BASS_STRETCH_CENTS_PER_OCTAVE_MAX) {
        LOGD("ExtraBassCentsPerOctave %.2f is out of range (%.2f:%.2f). Normalizing.",
             centsPerOctaveBass, EXTRA_BASS_STRETCH_CENTS_PER_OCTAVE_MIN,
             EXTRA_BASS_STRETCH_CENTS_PER_OCTAVE_MAX);
        centsPerOctaveBass = std::max(
                std::min(centsPerOctaveBass, EXTRA_BASS_STRETCH_CENTS_PER_OCTAVE_MAX),
                EXTRA_BASS_STRETCH_CENTS_PER_OCTAVE_MIN);
    }
    notesToStretchBass = (int) weights[position++];
    if (notesToStretchBass < EXTRA_BASS_STRETCH_OCTAVES_MIN ||
        notesToStretchBass > EXTRA_BASS_STRETCH_OCTAVES_MAX) {
        LOGD("ExtraBassOctaves %d is out of range (%d:%d). Normalizing.", notesToStretchBass,
             EXTRA_BASS_STRETCH_OCTAVES_MIN, EXTRA_BASS_STRETCH_OCTAVES_MAX);
        notesToStretchBass = std::max(std::min(notesToStretchBass, EXTRA_BASS_STRETCH_OCTAVES_MAX),
                                      EXTRA_BASS_STRETCH_OCTAVES_MIN);
    }
}

unsigned int IntervalWeights::get(double *weights) {
    unsigned int position = 0;
    memcpy(weights + position, octaveWeights, sizeof(double) * OCTAVE_WEIGHTS_LEN);
    position += OCTAVE_WEIGHTS_LEN;
    memcpy(weights + position, twelfthWeights, sizeof(double) * TWELFTH_WEIGHTS_LEN);
    position += TWELFTH_WEIGHTS_LEN;
    memcpy(weights + position, doubleOctaveWeights, sizeof(double) * DOUBLE_OCTAVE_WEIGHTS_LEN);
    position += DOUBLE_OCTAVE_WEIGHTS_LEN;
    memcpy(weights + position, nineteenthWeights, sizeof(double) * NINETEENTH_WEIGHTS_LEN);
    position += NINETEENTH_WEIGHTS_LEN;
    memcpy(weights + position, tripleOctaveWeights, sizeof(double) * TRIPLE_OCTAVE_WEIGHTS_LEN);
    position += TRIPLE_OCTAVE_WEIGHTS_LEN;
    memcpy(weights + position, fifthWeights, sizeof(double) * FIFTH_WEIGHTS_LEN);
    position += FIFTH_WEIGHTS_LEN;
    memcpy(weights + position, fourthWeights, sizeof(double) * FOURTH_WEIGHTS_LEN);
    position += FOURTH_WEIGHTS_LEN;
    weights[position++] = centsPerOctaveTreble;
    weights[position++] = notesToStretchTreble;
    weights[position++] = centsPerOctaveBass;
    weights[position++] = notesToStretchBass;
    return position;
}

void IntervalWeights::applyStretchOffsets(double *delta) {
    double stretchOffset[NOTES_ON_PIANO] = {0.0};
    for (int i = 0; i < notesToStretchBass; ++i) {
        stretchOffset[i] = -(centsPerOctaveBass / 12.0) *
                           (notesToStretchBass + 1 - i);
    }
    for (int i = NOTES_ON_PIANO - notesToStretchTreble + 1;
         i < NOTES_ON_PIANO; ++i) {
        stretchOffset[i] =
                (centsPerOctaveTreble / 12.0) *
                (i - (NOTES_ON_PIANO - notesToStretchTreble));
    }
    for (int i = 0; i < NOTES_ON_PIANO; ++i) {
        delta[i] += stretchOffset[i];
    }
}
