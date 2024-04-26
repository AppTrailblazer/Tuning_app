#include "PeakFinder.h"
#include "MathUtils.h"
#include "ToneDetectorConstants.h"

unsigned int
PeakFinder::findPeaks(double *values, double *freqs, int *indexes, double *x, double *y,
                      double SlopeThreshold, double AmpThreshold, bool doSmoothing, int n) {
    double ysmooth[n];
    memset(ysmooth, 0, sizeof(double) * n);
    double maxy = 1;
    double maxysmooth = 1;

    if (doSmoothing) {
        double temp[n];
        memcpy(temp, y, sizeof(double) * n);
        MathUtils::fastsmooth(temp, ysmooth, 3, 3, 1, n);

        maxy = *std::max_element(temp, temp + n);
        maxysmooth = *std::max_element(ysmooth, ysmooth + n);
    } else {
        memcpy(ysmooth, y, sizeof(double) * n);
    }

    double dy[2048] = {0.0};
    MathUtils::deriv(ysmooth, &dy[0], n);

    unsigned int peak = 0;

    // Reduce threshold for the lower height of smoothed peaks
    double thresholdScaled = AmpThreshold * maxysmooth / maxy;

    for (int j = 1; j <= n - 2 && peak < MAX_PEAKS; j++) {
        if (MathUtils::sgn(dy[j]) > MathUtils::sgn(dy[j + 1]) &&
            dy[j] - dy[j + 1] > SlopeThreshold &&
            (ysmooth[j] > thresholdScaled || ysmooth[j + 1] > thresholdScaled)) {

            if (ysmooth[j] > ysmooth[j + 1] || (j == n - 2)) {
                double c1 = (x[j + 1] - x[j]) * log(ysmooth[j + 1] / ysmooth[j - 1]);
                double c2 = 2 * (log((pow(ysmooth[j], 2)) / (ysmooth[j + 1] * ysmooth[j - 1])));
                double freq = x[j] + c1 / c2;
                freqs[peak] = freq;
                values[peak] = ysmooth[j] * maxy / maxysmooth;
                indexes[peak] = j;
            } else {
                double c1 = (x[j + 1] - x[j]) * log(ysmooth[j + 2] / ysmooth[j]);
                double c2 = 2 * (log((pow(ysmooth[j + 1], 2)) / (ysmooth[j + 2] * ysmooth[j])));
                double freq = x[j + 1] + c1 / c2;
                freqs[peak] = freq;
                values[peak] = ysmooth[j + 1] * maxy / maxysmooth;
                indexes[peak] = j + 1;
            }
            peak++;
        }
    }
    return peak;

}
