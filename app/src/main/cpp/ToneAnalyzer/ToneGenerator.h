#ifndef PIANOMETER_ANDROID_TONEGENERATOR_H
#define PIANOMETER_ANDROID_TONEGENERATOR_H

#include "ToneDetectorConstants.h"
#include "PianoKeyFrequencies.h"

#define TONE_PARTIALS 8

class ToneGenerator {
public:
    ToneGenerator(PianoKeyFrequencies *pianoKeyFrequencies);

    void setAudioFrequency(int frequency);

    void
    setTrebleBassOptions(float trebleVolume, short trebleEdge, float bassVolume, short bassEdge);

    void initTone(int noteZeroIndexed);

    int generateTone(short *buffer, int bufferLen);

    void getVolumeMultiplier(float *buffer);

    void getHarmonicsAmplitudes(float *buffer);

    void getPlaybackPartials(short *buffer);

private:
    PianoKeyFrequencies *pianoKeyFrequencies;

    int fs = 48000;

    float baselineVolume = 0.1f;

    float trebleVolume = 1.0f;
    float bassVolume = 2.0f;
    short trebleEdge = 75;
    short bassEdge = 18;
    std::array<float, NOTES_ON_PIANO> volumeMultiplier{};

    float harmonicAmplitudes[NOTES_ON_PIANO][10] = {0.0f};
    float oscilA[NOTES_ON_PIANO] = {0.0f};
    float oscilH[NOTES_ON_PIANO] = {0.0f};
    float oscilK[NOTES_ON_PIANO] = {0.0f};
    float oscilLogN[10] = {0.0f};

    int currentPartials = 0;
    float k1[TONE_PARTIALS] = {0.0f}, k2[TONE_PARTIALS] = {0.0f};
    float z1[TONE_PARTIALS] = {0.0f}, z2[TONE_PARTIALS] = {0.0f};
    float a[TONE_PARTIALS] = {0.0f}, b[TONE_PARTIALS] = {0.0f}, c[TONE_PARTIALS] = {0.0f}, d[TONE_PARTIALS] = {0.0f}, e[TONE_PARTIALS] = {0.0f};

    short playbackPartials[TONE_PARTIALS][NOTES_ON_PIANO] = {0};

    float ra(int note, int partial);

    void computeVolumeMultiplier();

    void computeHarmonicAmplitudes();

    void computePlaybackPartials();

    void computeOscillations();
};


#endif //PIANOMETER_ANDROID_TONEGENERATOR_H
