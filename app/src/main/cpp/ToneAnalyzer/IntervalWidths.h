#ifndef PIANOTUNING_INTERVALWIDTHS_H
#define PIANOTUNING_INTERVALWIDTHS_H


#include "PianoKeyFrequencies.h"

class IntervalWidths {
public:
    IntervalWidths(PianoKeyFrequencies *pianoKeyFrequencies) {
        this->pianoKeyFrequencies = pianoKeyFrequencies;
    }

    void computeWidths(
                       double peakHeightsCorrected[88][16],
                       bool useCents,
                       double result[32 * 88]);

private:
    PianoKeyFrequencies *pianoKeyFrequencies;

    double intervalWidth(double a, double b, bool useCents);


    void computeOctaveWidth(
            double peakHeightsCorrected[88][16],
            bool useCents,
            std::array<std::array<double, 88>, 5> &beatrate,
            std::array<std::array<double, 88>, 5> &strength
    );

    void computeFifthWidth(
            double peakHeightsCorrected[88][16],
            bool useCents,
            std::array<std::array<double, 88>, 2> &beatrate,
            std::array<std::array<double, 88>, 2> &strength
    );

    void computeFourthWidth(
            double peakHeightsCorrected[88][16],
            bool useCents,
            std::array<std::array<double, 88>, 2> &beatrate,
            std::array<std::array<double, 88>, 2> &strength
    );

    void computeTwelfthWidth(
            double peakHeightsCorrected[88][16],
            bool useCents,
            std::array<std::array<double, 88>, 3> &beatrate,
            std::array<std::array<double, 88>, 3> &strength
    );

    void computeDoubleOctaveWidth(
            double peakHeightsCorrected[88][16],
            bool useCents,
            std::array<std::array<double, 88>, 2> &beatrate,
            std::array<std::array<double, 88>, 2> &strength
    );

    void computeTripleOctaveWidth(
            double peakHeightsCorrected[88][16],
            bool useCents,
            std::array<std::array<double, 88>, 1> &beatrate,
            std::array<std::array<double, 88>, 1> &strength
    );

    void computeNineteenthWidth(
            double peakHeightsCorrected[88][16],
            bool useCents,
            std::array<std::array<double, 88>, 1> &beatrate,
            std::array<std::array<double, 88>, 1> &strength
    );
};


#endif //PIANOTUNING_INTERVALWIDTHS_H
