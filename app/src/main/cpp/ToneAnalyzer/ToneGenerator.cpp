#include "ToneGenerator.h"
#include "ToneDetector.h"
#include "debug_util.h"

#include <cmath>
#include <string>
#include <climits>

#define CLAMP(X, L, U) std::min(std::max(X, L), U)

ToneGenerator::ToneGenerator(PianoKeyFrequencies *pianoKeyFrequencies) {
    this->pianoKeyFrequencies = pianoKeyFrequencies;
    computeVolumeMultiplier();
    computeOscillations();
    computeHarmonicAmplitudes();
    computePlaybackPartials();
}

void ToneGenerator::setAudioFrequency(int frequency) {
    fs = frequency;
}

void ToneGenerator::setTrebleBassOptions(float trebleVolume, short trebleEdge, float bassVolume,
                                         short bassEdge) {
    this->trebleVolume = trebleVolume;
    this->trebleEdge = trebleEdge;
    this->bassVolume = bassVolume;
    this->bassEdge = bassEdge;
    computeVolumeMultiplier();
}

void ToneGenerator::computeVolumeMultiplier() {
    volumeMultiplier.fill(1.0f);
    for (int i = 0; i <= bassEdge; ++i) {
        volumeMultiplier[i] =
                bassVolume +
                (1 - bassVolume) * sinf(i * PI / 2 / (((float) bassEdge + 1.0f) - 0.5f));
    }
    float trebleDivider = PI / 2 / ((float) (NOTES_ON_PIANO - trebleEdge - 1) + 0.5f);
    for (int i = trebleEdge; i < NOTES_ON_PIANO; ++i) {
        float shift = i - trebleEdge;
        float base = NOTES_ON_PIANO - trebleEdge + shift;
        volumeMultiplier[i] = trebleVolume + (1 - trebleVolume) * sinf(base * trebleDivider);
    }
}

void ToneGenerator::computeHarmonicAmplitudes() {
    memset(harmonicAmplitudes, 0, NOTES_ON_PIANO * 10 * sizeof(float));
    for (int i = 0; i < NOTES_ON_PIANO; ++i) {
        for (int j = 0; j < 10; ++j) {
            harmonicAmplitudes[i][j] = powf(10, oscilA[i] * powf(oscilLogN[j] - oscilH[i], 2) +
                                                oscilK[i]);
        }
    }
}

void ToneGenerator::computePlaybackPartials() {
    memset(playbackPartials, 0, NOTES_ON_PIANO * 8 * sizeof(short));
    for (int i = 9; i < NOTES_ON_PIANO; ++i) {
        playbackPartials[0][i] = 1;
    }
    for (int i = 2; i < 70; ++i) {
        playbackPartials[1][i] = 2;
    }
    for (int i = 0; i < 61; ++i) {
        playbackPartials[2][i] = 3;
    }
    for (int i = 0; i < 55; ++i) {
        playbackPartials[3][i] = 4;
    }
    for (int i = 0; i < 50; ++i) {
        playbackPartials[4][i] = 5;
    }
    for (int i = 0; i < 45; ++i) {
        playbackPartials[5][i] = 6;
    }
    for (int i = 0; i < 39; ++i) {
        playbackPartials[6][i] = 7;
    }
    for (int i = 0; i < 30; ++i) {
        playbackPartials[7][i] = 8;
    }
    for (int i = 0; i < 21; ++i) {
        playbackPartials[1][i] = 9;
    }
    for (int i = 0; i < 15; ++i) {
        playbackPartials[0][i] = 10;
    }
}

void ToneGenerator::getVolumeMultiplier(float *buffer) {
    memcpy(buffer, volumeMultiplier.data(), NOTES_ON_PIANO * sizeof(float));
}

void ToneGenerator::getHarmonicsAmplitudes(float *buffer) {
    memcpy(buffer, harmonicAmplitudes, NOTES_ON_PIANO * 10 * sizeof(float));
}

void ToneGenerator::getPlaybackPartials(short *buffer) {
    memcpy(buffer, playbackPartials, NOTES_ON_PIANO * 8 * sizeof(short));
}

float ToneGenerator::ra(int noteZeroIndexed, int partial) {
    ASSERT_RANGE(noteZeroIndexed, 0, NOTES_ON_PIANO - 1);
    float partialFrequency = (float) (pianoKeyFrequencies->frequency(noteZeroIndexed) *
                                      (partial + 1));
    float partialFrequencySquare = partialFrequency * partialFrequency;
    float k1 = powf(12200.0, 2.0f);
    float k2 = powf(20.6, 2.0f);
    float k3 = powf(107.7, 2.0f);
    float k4 = powf(737.9, 2.0f);
    float raNumerator = k1 * powf(partialFrequency, 4.0f);
    float raDenominator = (partialFrequencySquare + k2) * (partialFrequencySquare + k1) *
                          sqrtf((partialFrequencySquare + k3) * (partialFrequencySquare + k4));
    return raNumerator / raDenominator;
}

void ToneGenerator::computeOscillations() {
    for (int i = 0; i < NOTES_ON_PIANO; ++i) {
        auto note = (float) (i + 1);
        oscilA[i] = -0.0028f * powf(note, 2.0f) + 0.2816f * note - 9.5986f;
        oscilH[i] = -0.0172f * note + 0.6826f;
        // oscilK is 0
    }
    for (int i = 0; i < 10; ++i) {
        oscilLogN[i] = log10f((float) (i + 1));
    }
}

void ToneGenerator::initTone(int noteZeroIndexed) {
    ASSERT_RANGE(noteZeroIndexed, 0, NOTES_ON_PIANO - 1);
    std::vector<short> targetPartials(TONE_PARTIALS);
    short partialsCount = 0;
    for (int i = 0; i < TONE_PARTIALS; ++i) {
        if (playbackPartials[i][noteZeroIndexed] != 0) {
            targetPartials[partialsCount++] = playbackPartials[i][noteZeroIndexed] - 1;
        }
    }

    std::vector<float> frequencies(partialsCount);
    for (int i = 0; i < partialsCount; ++i) {
        frequencies[i] = pianoKeyFrequencies->targetFrequency(targetPartials[i],
                                                              noteZeroIndexed);
    }

    std::vector<float> amplitudes(partialsCount);
    float amplitudesSum = 0.0f;
    for (int i = 0; i < partialsCount; ++i) {
        amplitudes[i] = harmonicAmplitudes[noteZeroIndexed][targetPartials[i]];
        amplitudesSum += amplitudes[i];
    }

    std::vector<float> impulse(partialsCount);
    for (int i = 0; i < partialsCount; ++i) {
        impulse[i] = amplitudes[i] / amplitudesSum * (1 - ra(noteZeroIndexed, 0)) * baselineVolume *
                     volumeMultiplier[noteZeroIndexed];
    }

    memset(k1, 0, TONE_PARTIALS * sizeof(float));
    memset(k2, 0, TONE_PARTIALS * sizeof(float));
    for (int i = 0; i < partialsCount; ++i) {
        k1[i] = cosf(2 * PI * frequencies[i] / fs);
        k2[i] = 2.0f * sinf(PI * frequencies[i] / fs);
    }

    memset(z1, 0, TONE_PARTIALS * sizeof(float));
    memset(z2, 0, TONE_PARTIALS * sizeof(float));
    memset(a, 0, TONE_PARTIALS * sizeof(float));
    memset(b, 0, TONE_PARTIALS * sizeof(float));
    memset(c, 0, TONE_PARTIALS * sizeof(float));
    memset(d, 0, TONE_PARTIALS * sizeof(float));
    memset(e, 0, TONE_PARTIALS * sizeof(float));

    for (int j = 0; j < partialsCount; ++j) {
        a[j] = z1[j] + impulse[j];
        d[j] = k1[j] * a[j];
        e[j] = d[j] + z2[j];
        c[j] = k1[j] * e[j];
        b[j] = c[j] - a[j];

        z1[j] = e[j];
        z2[j] = b[j];
    }
    currentPartials = partialsCount;
}

int ToneGenerator::generateTone(short *buffer, int bufferLen) {
    ASSERT_RANGE(currentPartials, 1, TONE_PARTIALS);

    float aSum = 0.0f;
    for (int j = 0; j < currentPartials; ++j) {
        aSum += a[j];
    }
    buffer[0] = (SHRT_MAX * CLAMP(aSum, -1.0f, 1.0f));

    for (int i = 1; i < bufferLen; ++i) {
        aSum = 0.0f;
        for (int j = 0; j < currentPartials; ++j) {
            a[j] = z1[j];
            d[j] = k1[j] * a[j];
            e[j] = d[j] + z2[j];
            c[j] = k1[j] * e[j];
            b[j] = c[j] - a[j];

            z1[j] = e[j];
            z2[j] = b[j];
            aSum += a[j];
        }
        buffer[i] = (SHRT_MAX * CLAMP(aSum, -1.0f, 1.0f));
    }

    for (int j = 0; j < currentPartials; ++j) {
        a[j] = z1[j];
        d[j] = k1[j] * a[j];
        e[j] = d[j] + z2[j];
        c[j] = k1[j] * e[j];
        b[j] = c[j] - a[j];

        z1[j] = e[j];
        z2[j] = b[j];
    }

    return bufferLen;
}


