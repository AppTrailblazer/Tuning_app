#ifndef PIANOTUNING_PEAKFINDER_H
#define PIANOTUNING_PEAKFINDER_H

class PeakFinder {
public:
    unsigned int
    findPeaks(double *values, double *freqs, int *indexes, double *x, double *y,
              double SlopeThreshold, double AmpThreshold, bool doSmoothing, int n);
};


#endif //PIANOTUNING_PEAKFINDER_H
