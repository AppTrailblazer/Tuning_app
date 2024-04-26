#ifndef PIANOTUNING_BAND_PASS_FILTER_H
#define PIANOTUNING_BAND_PASS_FILTER_H
#include "math.h"

class BandPassFilter {
public:
    BandPassFilter(int sampleRate = 16000) : sampleRate(sampleRate) {
        delay[0] = delay[1] = 0.0;
    }

    void setParam(double frequency, double bandwidth) {
        double w0 = 2.0 * M_PI * frequency / sampleRate;
        double alpha = sin(w0) * sinh(log(2.0) * 0.5 * bandwidth * w0 / sin(w0));
        b0 = alpha;
        //b1 = 0.0;
        //b2 = -alpha;
        double a0inv = 1.0 / (1.0 + alpha);
        neg_a[0] = 2.0 * cos(w0);
        neg_a[1] = -1.0 + alpha;

        b0 *= a0inv;
        neg_a[0] *= a0inv;
        neg_a[1] *= a0inv;
        //delay[0] = delay[1] = 0.0;
    }

    // direct form 2 transposed
    // assumes a0 = 1, b1 = 0, b2 = -b0
    double filter(double x) {
        double b0x = b0 * x;
        double y = b0x + delay[0];
        delay[0] = neg_a[0] * y + delay[1];
        delay[1] = -b0x + neg_a[1] * y;
        return y;
    }

private:
    int sampleRate;
    double b0, neg_a[2] = {0.0};
    double delay[2] = {0.0};
};
#endif //PIANOTUNING_BAND_PASS_FILTER_H