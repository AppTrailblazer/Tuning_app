#ifndef PIANOMETER_ANDROID_INTERVALWEIGHTS_H
#define PIANOMETER_ANDROID_INTERVALWEIGHTS_H

#define OCTAVE_WEIGHTS_LEN 5
#define TWELFTH_WEIGHTS_LEN 3
#define DOUBLE_OCTAVE_WEIGHTS_LEN 2
#define NINETEENTH_WEIGHTS_LEN 1
#define TRIPLE_OCTAVE_WEIGHTS_LEN 1
#define FIFTH_WEIGHTS_LEN 2
#define FOURTH_WEIGHTS_LEN 2
#define THIRD_WEIGHTS_LEN 2
#define EXTRA_TREBLE_STRETCH_CENTS_PER_OCTAVE_DEFAULT 0.0
#define EXTRA_TREBLE_STRETCH_CENTS_PER_OCTAVE_MIN -3.0
#define EXTRA_TREBLE_STRETCH_CENTS_PER_OCTAVE_MAX 3.0
#define EXTRA_TREBLE_STRETCH_OCTAVES_DEFAULT 18
#define EXTRA_TREBLE_STRETCH_OCTAVES_MIN 6
#define EXTRA_TREBLE_STRETCH_OCTAVES_MAX 39
#define EXTRA_BASS_STRETCH_CENTS_PER_OCTAVE_DEFAULT 0.0
#define EXTRA_BASS_STRETCH_CENTS_PER_OCTAVE_MIN -3.0
#define EXTRA_BASS_STRETCH_CENTS_PER_OCTAVE_MAX 3.0
#define EXTRA_BASS_STRETCH_OCTAVES_DEFAULT 12
#define EXTRA_BASS_STRETCH_OCTAVES_MIN 6
#define EXTRA_BASS_STRETCH_OCTAVES_MAX 32

class IntervalWeights {
public:
    IntervalWeights();

    unsigned int get(double *weights);

    void set(const double *weights);

    double octave21() { return octaveWeights[0]; };

    double octave42() { return octaveWeights[1]; };

    double octave63() { return octaveWeights[2]; };

    double octave84() { return octaveWeights[3]; };

    double octave105() { return octaveWeights[4]; };

    double fifth32() { return fifthWeights[0]; };

    double fifth64() { return fifthWeights[1]; };

    double fourth43() { return fourthWeights[0]; };

    double fourth86() { return fourthWeights[1]; };

    double nineteenth61() { return nineteenthWeights[0]; };

    double doubleOctave41() { return doubleOctaveWeights[0]; };

    double doubleOctave82() { return doubleOctaveWeights[1]; };

    double tripleOctave() { return tripleOctaveWeights[0]; };

    double twelfth31() { return twelfthWeights[0]; };

    double twelfth62() { return twelfthWeights[1]; };

    double twelfth93() { return twelfthWeights[2]; };

    double third54() { return thirdWeights[0]; };

    double third65() { return thirdWeights[1]; };

    void applyStretchOffsets(double *delta);

private:
    double octaveWeights[OCTAVE_WEIGHTS_LEN] = {1.0 / 2.0, 1.0 / 4.0, 1.0 / 2.0, 1.0 / 1.0,
                                                1.0 / 0.25};
    double twelfthWeights[TWELFTH_WEIGHTS_LEN] = {1.0 / 7.0, 1.0 / 4.0, 1.0 / 1.0};
    double doubleOctaveWeights[DOUBLE_OCTAVE_WEIGHTS_LEN] = {1.0 / 1.0, 1.0 / 0.5};
    double nineteenthWeights[NINETEENTH_WEIGHTS_LEN] = {1.0 / 1.0};
    double tripleOctaveWeights[TRIPLE_OCTAVE_WEIGHTS_LEN] = {1.0 / 1.0};
    double fifthWeights[FIFTH_WEIGHTS_LEN] = {1.0 / 3.0, 1.0 / 1.0};
    double fourthWeights[FOURTH_WEIGHTS_LEN] = {1.0 / 1.0, 1.0 / 0.25};
    double thirdWeights[THIRD_WEIGHTS_LEN] = {1.0 / 0.015, 1.0 / 0.008};

    double centsPerOctaveTreble = EXTRA_TREBLE_STRETCH_CENTS_PER_OCTAVE_DEFAULT;
    int notesToStretchTreble = EXTRA_TREBLE_STRETCH_OCTAVES_DEFAULT;

    double centsPerOctaveBass = EXTRA_BASS_STRETCH_CENTS_PER_OCTAVE_DEFAULT;
    int notesToStretchBass = EXTRA_BASS_STRETCH_OCTAVES_DEFAULT;
};


#endif //PIANOMETER_ANDROID_INTERVALWEIGHTS_H
