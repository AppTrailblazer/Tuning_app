#include <cmath>
#include <vector>
#include <sstream>

#include "Spinner.h"
#include "debug_util.h"

Spinner::Spinner(int id, int sampleRate, int samplesPerUpdate, double bpfBandwidth) {
    this->id = id;

    std::ostringstream os;
    os << "Spinner #" << id;

    this->tag = os.str();
    this->sampleRate = sampleRate;
    this->samplesPerUpdate = samplesPerUpdate;
    this->bpfBandwidth = bpfBandwidth;
    this->threshold = 0.0;
    bpf1Delay[0] = bpf1Delay[1] = 0.0;
    bpf2Delay[0] = bpf2Delay[1] = 0.0;
    y1 = 0.0;
    dataSum = dataSumBPF = 0.0F;
    referencePhase = 0.0;
    valid = false;
    processedSamples = 0;
    setFrequency(440.0);
}

void Spinner::setFrequency(double freq) {
    cyclesPerSample = freq / sampleRate;

    // Bandpass filter coefficients
    double w0 = 2.0 * M_PI * freq / sampleRate;
    double alpha = sin(w0) * sinh(log(2.0) * 0.5 * bpfBandwidth * w0 / sin(w0));
    bpf_b0 = alpha;
    //b1 = 0.0;
    //b2 = -alpha;
    double a0inv = 1.0 / (1.0 + alpha);
    bpf_neg_a[0] = 2.0 * cos(w0);
    bpf_neg_a[1] = -1.0 + alpha;

    bpf_b0 *= a0inv;
    bpf_neg_a[0] *= a0inv;
    bpf_neg_a[1] *= a0inv;
}

void Spinner::setDetectionParam(double threshold) {
    this->threshold = threshold;
}

// 2nd order bandpass filter, direct form 2 transposed
// assumes a0 = 1, b1 = 0, b2 = -b0
static inline double bandpassFilter(double x, double delay[2], double b0, const double a[2]) {
    double b0x = b0 * x;
    double y = b0x + delay[0];
    delay[0] = a[0] * y + delay[1];
    delay[1] = -b0x + a[1] * y;
    return y;
}

std::vector<float> Spinner::getPhase(const short *samples, int numSamples) {
    std::vector<float> output;

    for (int i = 0; i < numSamples; i++) {
        float x = (float) samples[i];
        // apply bandpass filter twice
        double yd = bandpassFilter(x, bpf1Delay, bpf_b0, bpf_neg_a);
        yd = bandpassFilter(yd, bpf2Delay, bpf_b0, bpf_neg_a);
        float y = (float) yd;
        // average amplitude by integrating abs(data)
        dataSum += fabsf(x);
        dataSumBPF += fabsf(y);

        // check for rising zero crossing
        if (y1 < 0.0 && y > 0.0) {
            // save zero crossing values
            zeroCrossingy1 = y1;
            zeroCrossingy = y;
            zeroCrossingRefPhase = (float) referencePhase;
            valid = true;
        }
        // sample delay
        y1 = y;

        processedSamples++;
        if (processedSamples == samplesPerUpdate) {
            if (valid) {
                float dataSumRatio = dataSumBPF / dataSum;
                if (dataSumRatio > threshold && dataSumRatio < 1.4F) {
                    // interpolate zero crossing sample position
                    float zeroCrossing = zeroCrossingy1 / (zeroCrossingy1 - zeroCrossingy);
                    // phase difference between zero crossing and reference
                    float phase = -(zeroCrossingRefPhase +
                                    zeroCrossing * (float) cyclesPerSample);
                    // wrap to [0..1]
                    phase = fmod(phase, 1.0F);
                    if (phase < 0) {
                        phase += 1.0F;
                    }
                    output.push_back(phase);
                } else {
                    output.push_back(SPINNER_INACTIVE);
                }
                valid = false;
            }
            processedSamples = 0;
            dataSum = dataSumBPF = 0.0F;
        }

        // increment phase
        referencePhase += cyclesPerSample;
        // wrap to [0..1]
        if (referencePhase >= 1.0) {
            referencePhase -= 1.0;
        }
    }
    return output;
}
