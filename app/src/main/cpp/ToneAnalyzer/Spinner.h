#ifndef TUNING_PROGRAM_SPINNER_H
#define TUNING_PROGRAM_SPINNER_H

#include <string>
#include <vector>

#define SPINNER_INACTIVE -2.0F

class Spinner {

public:
    Spinner(int id, int sampleRate, int samplesPerUpdate, double bpfBandwidth);

    void setFrequency(double freq);

    void setDetectionParam(double threshold);

    std::vector<float> getPhase(const short *samples, int numSamples);

private:
    int id;
    std::string tag;
    int sampleRate;
    // number of samples between phase estimates
    int samplesPerUpdate;
    double referencePhase;
    double cyclesPerSample;
    float zeroCrossingy1, zeroCrossingy, zeroCrossingRefPhase;
    bool valid;
    int processedSamples;

    double bpfBandwidth;
    double threshold;
    // bandpass filter coefficients
    double bpf_b0, bpf_neg_a[2] = {0.0};
    // filter delay
    double bpf1Delay[2] = {0.0};
    double bpf2Delay[2] = {0.0};
    // filtered sample delay
    float y1;
    // accumulator for sum of absolute values
    float dataSum, dataSumBPF;
};

#endif //TUNING_PROGRAM_SPINNER_H
