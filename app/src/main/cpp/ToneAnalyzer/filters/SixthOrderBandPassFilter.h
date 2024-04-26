#ifndef PIANOTUNING_SIXTH_ORDER_BAND_PASS_FILTER_H
#define PIANOTUNING_SIXTH_ORDER_BAND_PASS_FILTER_H
#define NoTaps 7

/**
 * Add bandpass filter before downsampling if the sample rate is 44100 or 48000,
 * with cutoffs at 70Hz and 8000Hz (to prevent aliasing).
 *
 * If the sample rate is 16000Hz, add only a highpass filter at 70Hz for low frequency noise.
 */
class SixthOrderBandPassFilter {
private:
    double a_taps[3][NoTaps] = {{1, -2.94502385834109, 2.89154852455463, -0.946504453847504, 0,                0,                  0}, //for sampling rate 16000Hz
                                {1, -3.78766331903057, 5.87964733809593, -4.98486708881192,  2.56612128488039, -0.757788427784563, 0.0845508346779937},//for sampling rate 44100Hz
                                {1, -3.96187082898738, 6.48182016136178, -5.77333210382906,  3.05735007817004, -0.912543820516278, 0.108576907926806}};//for sampling rate 48000Hz
    double b_taps[3][NoTaps] = {{0.972884604592902,  -2.91865381377871, 2.91865381377871,   -0.972884604592902, 0,                 0, 0},//for sampling rate 16000Hz
                                {0.0765618666802180, 0,                 -0.229685600040654, 0,                  0.229685600040654, 0, -0.0765618666802180},//for sampling rate 44100Hz
                                {0.0625238457868013, 0,                 -0.187571537360404, 0,                  0.187571537360404, 0, -0.0625238457868013}};//for sampling rate 48000Hz


    /**
     * Numerator (b) and denominator (a) polynomials of the IIR filter
     * Default to 16000 Hz
     */
    double *a = &a_taps[0][0];
    double *b = &b_taps[0][0];

    double x[NoTaps] = {0};
    double y[NoTaps] = {0};
public:
    void filter(short *signal, int size);

    void setSamplingRate(double samplingRate);
};
#endif //PIANOTUNING_SIXTH_ORDER_BAND_PASS_FILTER_H