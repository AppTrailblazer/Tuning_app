#include <array>
#include "IntervalWidths.h"
#include "MathUtils.h"

double IntervalWidths::intervalWidth(double a, double b, bool useCents) {
    if (useCents) {
        return 1200.0 * MathUtils::log2(b / a);
    } else {
        return b - a;
    }
}

void IntervalWidths::computeOctaveWidth(
        double peakHeightsCorrected[88][16],
        bool useCents,
        std::array<std::array<double, 88>, 5> &beatrate,
        std::array<std::array<double, 88>, 5> &strength) {
    for (int m = 1; m <= 5; ++m) {
        for (int n = 0; n < 76; ++n) {
            int i1 = (2 * m) - 1, i2 = n, i3 = m - 1, i4 = n + 12;
            beatrate[m - 1][n] =
                    intervalWidth(pianoKeyFrequencies->targetFrequency(i1, i2),
                                  pianoKeyFrequencies->targetFrequency(i3, i4), useCents);
            strength[m - 1][n] = peakHeightsCorrected[i2][i1] * peakHeightsCorrected[i4][i3];
        }
    }
}

void IntervalWidths::computeFifthWidth(
        double peakHeightsCorrected[88][16],
        bool useCents,
        std::array<std::array<double, 88>, 2> &beatrate,
        std::array<std::array<double, 88>, 2> &strength) {
    for (int m = 1; m <= 2; ++m) {
        for (int n = 0; n < 79; ++n) {
            int i1 = (3 * m) - 1, i2 = n, i3 = (2 * m) - 1, i4 = n + 7;
            beatrate[m - 1][n] =
                    intervalWidth(pianoKeyFrequencies->targetFrequency(i1, i2),
                                  pianoKeyFrequencies->targetFrequency(i3, i4), useCents);
            strength[m - 1][n] =
                    peakHeightsCorrected[i2][i1] * peakHeightsCorrected[i4][i3];
        }
    }
}

void IntervalWidths::computeFourthWidth(
        double peakHeightsCorrected[88][16],
        bool useCents,
        std::array<std::array<double, 88>, 2> &beatrate,
        std::array<std::array<double, 88>, 2> &strength) {
    for (int m = 1; m <= 2; ++m) {
        for (int n = 0; n < 81; ++n) {
            int i1 = (4 * m) - 1, i2 = n, i3 = (3 * m) - 1, i4 = n + 5;
            beatrate[m - 1][n] =
                    intervalWidth(pianoKeyFrequencies->targetFrequency(i1, i2),
                                  pianoKeyFrequencies->targetFrequency(i3, i4), useCents);
            strength[m - 1][n] =
                    peakHeightsCorrected[i2][i1] * peakHeightsCorrected[i4][i3];
        }
    }
}

void IntervalWidths::computeTwelfthWidth(
        double peakHeightsCorrected[88][16],
        bool useCents,
        std::array<std::array<double, 88>, 3> &beatrate,
        std::array<std::array<double, 88>, 3> &strength) {
    for (int m = 1; m <= 3; ++m) {
        for (int n = 0; n < 69; ++n) {
            int i1 = (3 * m) - 1, i2 = n, i3 = m - 1, i4 = n + 19;
            beatrate[m - 1][n] =
                    intervalWidth(pianoKeyFrequencies->targetFrequency(i1, i2),
                                  pianoKeyFrequencies->targetFrequency(i3, i4), useCents);
            strength[m - 1][n] =
                    peakHeightsCorrected[i2][i1] * peakHeightsCorrected[i4][i3];
        }
    }
}

void IntervalWidths::computeDoubleOctaveWidth(
        double peakHeightsCorrected[88][16],
        bool useCents,
        std::array<std::array<double, 88>, 2> &beatrate,
        std::array<std::array<double, 88>, 2> &strength) {
    for (int m = 1; m <= 2; ++m) {
        for (int n = 0; n < 64; ++n) {
            int i1 = (4 * m) - 1, i2 = n, i3 = m - 1, i4 = n + 24;
            beatrate[m - 1][n] =
                    intervalWidth(pianoKeyFrequencies->targetFrequency(i1, i2),
                                  pianoKeyFrequencies->targetFrequency(i3, i4), useCents);
            strength[m - 1][n] =
                    peakHeightsCorrected[i2][i1] * peakHeightsCorrected[i4][i3];
        }
    }
}

void IntervalWidths::computeTripleOctaveWidth(
        double peakHeightsCorrected[88][16],
        bool useCents,
        std::array<std::array<double, 88>, 1> &beatrate,
        std::array<std::array<double, 88>, 1> &strength) {
    for (int n = 0; n < 52; ++n) {
        int i1 = 7, i2 = n, i3 = 0, i4 = n + 36;
        beatrate[0][n] =
                intervalWidth(pianoKeyFrequencies->targetFrequency(i1, i2),
                              pianoKeyFrequencies->targetFrequency(i3, i4), useCents);
        strength[0][n] =
                peakHeightsCorrected[i2][i1] * peakHeightsCorrected[i4][i3];
    }
}

void IntervalWidths::computeNineteenthWidth(
        double peakHeightsCorrected[88][16],
        bool useCents,
        std::array<std::array<double, 88>, 1> &beatrate,
        std::array<std::array<double, 88>, 1> &strength) {
    for (int n = 0; n < 57; ++n) {
        int i1 = 5, i2 = n, i3 = 0, i4 = n + 31;
        beatrate[0][n] =
                intervalWidth(pianoKeyFrequencies->targetFrequency(i1, i2),
                              pianoKeyFrequencies->targetFrequency(i3, i4), useCents);
        strength[0][n] =
                peakHeightsCorrected[i2][i1] * peakHeightsCorrected[i4][i3];
    }
}

void IntervalWidths::computeWidths(double peakHeightsCorrected[88][16],
                                   bool useCents,
                                   double *result) {
    std::array<std::array<double, 88>, 5> octaveBeatrate{};
    std::array<std::array<double, 88>, 5> octaveStrength{};
    std::array<std::array<double, 88>, 2> fifthBeatrate{};
    std::array<std::array<double, 88>, 2> fifthStrength{};
    std::array<std::array<double, 88>, 2> fourthBeatrate{};
    std::array<std::array<double, 88>, 2> fourthStrength{};
    std::array<std::array<double, 88>, 3> twelfthBeatrate{};
    std::array<std::array<double, 88>, 3> twelfthStrength{};
    std::array<std::array<double, 88>, 2> doubleOctaveBeatrate{};
    std::array<std::array<double, 88>, 2> doubleOctaveStrength{};
    std::array<std::array<double, 88>, 1> tripleOctaveBeatrate{};
    std::array<std::array<double, 88>, 1> tripleOctaveStrength{};
    std::array<std::array<double, 88>, 1> nineteenthBeatrate{};
    std::array<std::array<double, 88>, 1> nineteenthStrength{};
    int resultIndex = 0;

    computeOctaveWidth(peakHeightsCorrected, useCents, octaveBeatrate,
                       octaveStrength);
    for (int i = 0; i < octaveBeatrate.size(); ++i) {
        for (int j = 0; j < octaveBeatrate[i].size(); ++j) {
            result[resultIndex++] = octaveBeatrate[i][j];
        }
    }
    for (int i = 0; i < octaveStrength.size(); ++i) {
        for (int j = 0; j < octaveStrength[i].size(); ++j) {
            result[resultIndex++] = octaveStrength[i][j];
        }
    }
    computeFifthWidth(peakHeightsCorrected, useCents, fifthBeatrate, fifthStrength);
    for (int i = 0; i < fifthBeatrate.size(); ++i) {
        for (int j = 0; j < fifthBeatrate[i].size(); ++j) {
            result[resultIndex++] = fifthBeatrate[i][j];
        }
    }
    for (int i = 0; i < fifthStrength.size(); ++i) {
        for (int j = 0; j < fifthStrength[i].size(); ++j) {
            result[resultIndex++] = fifthStrength[i][j];
        }
    }
    computeFourthWidth(peakHeightsCorrected, useCents, fourthBeatrate,
                       fourthStrength);
    for (int i = 0; i < fourthBeatrate.size(); ++i) {
        for (int j = 0; j < fourthBeatrate[i].size(); ++j) {
            result[resultIndex++] = fourthBeatrate[i][j];
        }
    }
    for (int i = 0; i < fourthStrength.size(); ++i) {
        for (int j = 0; j < fourthStrength[i].size(); ++j) {
            result[resultIndex++] = fourthStrength[i][j];
        }
    }
    computeTwelfthWidth(peakHeightsCorrected, useCents, twelfthBeatrate,
                        twelfthStrength);
    for (int i = 0; i < twelfthBeatrate.size(); ++i) {
        for (int j = 0; j < twelfthBeatrate[i].size(); ++j) {
            result[resultIndex++] = twelfthBeatrate[i][j];
        }
    }
    for (int i = 0; i < twelfthStrength.size(); ++i) {
        for (int j = 0; j < twelfthStrength[i].size(); ++j) {
            result[resultIndex++] = twelfthStrength[i][j];
        }
    }
    computeDoubleOctaveWidth(peakHeightsCorrected, useCents, doubleOctaveBeatrate,
                             doubleOctaveStrength);
    for (int i = 0; i < doubleOctaveBeatrate.size(); ++i) {
        for (int j = 0; j < doubleOctaveBeatrate[i].size(); ++j) {
            result[resultIndex++] = doubleOctaveBeatrate[i][j];
        }
    }
    for (int i = 0; i < doubleOctaveStrength.size(); ++i) {
        for (int j = 0; j < doubleOctaveStrength[i].size(); ++j) {
            result[resultIndex++] = doubleOctaveStrength[i][j];
        }
    }
    computeTripleOctaveWidth(peakHeightsCorrected, useCents, tripleOctaveBeatrate,
                             tripleOctaveStrength);
    for (int i = 0; i < tripleOctaveBeatrate.size(); ++i) {
        for (int j = 0; j < tripleOctaveBeatrate[i].size(); ++j) {
            result[resultIndex++] = tripleOctaveBeatrate[i][j];
        }
    }
    for (int i = 0; i < tripleOctaveStrength.size(); ++i) {
        for (int j = 0; j < tripleOctaveStrength[i].size(); ++j) {
            result[resultIndex++] = tripleOctaveStrength[i][j];
        }
    }
    computeNineteenthWidth(peakHeightsCorrected, useCents, nineteenthBeatrate,
                           nineteenthStrength);
    for (int i = 0; i < nineteenthBeatrate.size(); ++i) {
        for (int j = 0; j < nineteenthBeatrate[i].size(); ++j) {
            result[resultIndex++] = nineteenthBeatrate[i][j];
        }
    }
    for (int i = 0; i < nineteenthStrength.size(); ++i) {
        for (int j = 0; j < nineteenthStrength[i].size(); ++j) {
            result[resultIndex++] = nineteenthStrength[i][j];
        }
    }
}
