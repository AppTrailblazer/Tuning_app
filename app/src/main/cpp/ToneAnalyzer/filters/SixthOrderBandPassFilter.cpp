#include "SixthOrderBandPassFilter.h"

void SixthOrderBandPassFilter::filter(short *signal, int size) {
    for (int i = 0; i < size; i++) {
        for (int j = NoTaps - 1; j > 0; j--) {
            x[j] = x[j - 1];
        }
        x[0] = (double) signal[i];
        y[0] = b[0] * x[0];
        for (int j = 1; j < NoTaps; j++) {
            y[0] += b[j] * x[j] - a[j] * y[j];
        }
        signal[i] = (short) y[0];

        // Shift output values
        for (int j = NoTaps - 1; j > 0; j--) {
            y[j] = y[j - 1];
        }
    }
}

void SixthOrderBandPassFilter::setSamplingRate(double samplingRate) {
    if (samplingRate < 16500.0) {
        a = &a_taps[0][0];
        b = &b_taps[0][0];
    } else if (samplingRate < 44500.0) {
        a = &a_taps[1][0];
        b = &b_taps[1][0];
    } else {
        a = &a_taps[2][0];
        b = &b_taps[2][0];
    }
}