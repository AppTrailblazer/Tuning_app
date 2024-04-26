#ifndef ToneDetector_h
#define ToneDetector_h

#include <mutex>
#include <array>
#include <vector>
#include <stdio.h>
#include "kiss_fft.h"
#include "kiss_fftr.h"
#include "PeakFinder.h"
#include "SamplingRateConverter.h"
#include "ToneDetectorConstants.h"
#include "PianoKeyFrequencies.h"
#include "TuningPartials.h"
#include "IntervalWeights.h"
#include "filters/SixthOrderBandPassFilter.h"
#include "filters/BandPassFilter.h"

#define INTERNAL_FRAME_LEN 4096
#define RING_BUFFER_LEN 16384

#define BANDPASS_FILTER_BANDWIDTH 0.083

#define NOTE_CHANGE_AUTO 1
#define NOTE_CHANGE_STEP 2
#define NOTE_CHANGE_LOCK 3

template<typename T, std::size_t N>
void arrayShift(std::array <T, N> &array, T val) {
    for (int i = 0; i < N - 1; i++) {
        array[i] = array[i + 1];
    }
    array[N - 1] = val;
}

class ToneDetector {
public:
    ToneDetector();

    ~ToneDetector();

    void addData(short *buffer, int bufferLen);

    bool detectNotes();

    bool fastLoop();

    void processZeroCrossing();

    void runTcCalculator(bool clearCentsOffsetAvgPlot);

    void forceRecalculate() { calculateBxAndTc(true); }

    // Getter functions for parameters
    double getNum();

    void getSpinnerEnabled(bool *enabled);

    int getTargetPeakFreq(double *freq);

    bool isOffsetOver() { return offsetOver; }

    double getAngle() { return angle; }

    int getCurrentNote() { return currentNote; }

    int getNSC() { return NSC; } // note switch counter

    float getCentsOffsetAvg() { return centsOffsetAvgLPF; }

    float getCentsOffsetZCAvg();

    float getCentsOffsetCombined();

    int getCandidateNote() { return candidateNote; }

    double getQ() { return roundedQ; }

    int getAcTone() { return acTone; }

    double getRsq() { return rsq; }

    int getBx(int *bx);

    int getBy(double *by);

    int getBxfit(double *bxfit);

    int getTemperament(double *temperament);

    int getBave(double *bAve);

    int getDelta(double *delta);

    int getFFTResArray(float *fftArray);

    bool isTargetLengthCounted() { return isTargetLengthCount; }

    bool isQualityTestOk() { return qualityTestCombined; }

    void setNoteChangeMode(int mode) { noteChangeMode = mode; }

    int getNoteChangeMode() { return noteChangeMode; }

    void xcorr(float *in, double *res, int length);

    // For file writing & reading
    void setInharmonicity(const double *data);

    int getInharmonicity(double *data);

    void setDelta(const double *data);

    void setBxFit(const double *data);

    void setTemperament(const double *data);

    void setPeaksHeight(const double *data);

    int getPeaksHeight(double *data);

    void setWeights(const double *weights);

    void setFx(const double *fx);

    void setHarmonics(const double *harmonics);

    int getHarmonics(double *harmonics);

    void getIntervalWidths(double widths[32 * NOTES_ON_PIANO], bool useCents);

    void preparation();

    void setRecalculateTuning(bool recalculate) { isRecalculateTuningOn = recalculate; }

    int getFx(double *fx);

    void setCurrentNote(int note, bool relative, bool fromUser);

    void setNSC(int reset);

    void reset();

    void tcCalculatorP(double centsAvg[NOTES_ON_PIANO][16],
                       double peaksHeightCorrected[NOTES_ON_PIANO][16], double *out);

    void
    calculatePeakHeightsCorrected(double peakHeightsSmooth[NOTES_ON_PIANO][16],
                                  double out[NOTES_ON_PIANO][16]);

    // Pitch raise
    void startPitchRaiseMeasurement(const int *keys);

    bool isPitchRaiseMeasurementOn() { return isPitchRaiseMeasurementOnFlag; }

    void stopPitchRaiseMeasurement();

    // Calibration
    void setCalibrationFactor(double factor);

    double getCalibrationFactor() { return calibrationFactor; }

    // Pitch offset
    void setPitchOffsetFactor(double factor);

    double getPitchOffsetFactor() { return pianoKeyFrequencies.getOffsetFactor(); }

    void setInharmonicityWeight(double weight);

    void setInputSamplingRate(double samplingRate);

    // Spinner phase
    void getPhase(int partial, std::vector<float> *state);

    void skipPhases(int partial, int numberOfSkippedFrames);

    void setOverpullCents(const double *cents);

    PianoKeyFrequencies* getPianoKeyFrequenciesPtr();

private:
    double calculateCentsOffsetAvgPlot(int noteZeroIndexed);

    bool findOptimalDetectNotesBuffer(const short *buffer, unsigned int bufferLen, short *outBuffer,
                                      unsigned int outBufferLen);

    bool
    audioDataHasPianoKeyAttackAtTheSecondHalf(const short *buffer, const unsigned int bufferLen);

    void calculateCentsOffsetAvgPlotArray();

    float computeSpinnerAlpha(float dataSumRatio, float threshold);

    void getBuffer(short *buffer, int length, unsigned long long position);

    bool isSilence(short *buffer, unsigned int bufferLen);

    int getMaxIndex(double *values, int n);

    void calculateDiff(double *arr, double *diff, int len);

    int closestKey(double freq);

    int closestKeyP12(double freq);

    int adjustKeyForPitchRaise(int key);

    void calculateBxAndTc(bool force);

    void performPeakHeightsSmoothing();

    void
    bAnalysis(float *data, const double *peakFreqs, const double *peakValues, unsigned int nPeaks);

    void AddRowToSystem(int n1, int p1, int n2, int p2, double w, double width,
                        double M[NOTES_ON_PIANO][NOTES_ON_PIANO],
                        double peakHeights[NOTES_ON_PIANO][16],
                        double p_cents_pkf[NOTES_ON_PIANO][16]);

    void
    assembleSystemMatrix(double cents[NOTES_ON_PIANO][16], double peakHeights[NOTES_ON_PIANO][16],
                         double M[NOTES_ON_PIANO][NOTES_ON_PIANO]);

    bool qualityTest = false;
    bool isRecalculateTuningOn = true;

    float window[INTERNAL_FRAME_LEN] = {0.0};

    double getFreqRangeToneDetector(int freq);

    int centsOffsetCalc(float *dft, double maxAmplitudeToneDetector, double *centOffset);

    double scaleFcn(double offset);

    PeakFinder peakFinder;

    IntervalWeights intervalWeights;

    kiss_fftr_cfg cfgFastLoop;
    kiss_fftr_cfg cfgNoteDetector;
    kiss_fftr_cfg cfgXcorr;
    kiss_fftr_cfg cfgXcorri;
    kiss_fftr_cfg cfgXcorr2;
    kiss_fftr_cfg cfgXcorri2;
    int noteChangeMode = NOTE_CHANGE_AUTO;

    int previousNote = 0;
    int currentNote = 49;  // 1-based
    int previousNoteF = 49;

    int NSC = 0; // Note switch counter to trigger security message

    double roundedQ = 0;
    int acTone = 100;
    double rsq;
    int candidateNote = 49;
    double measuredPeakHeight[1024] = {0.0};
    int measuredPeakHeightLen;

    PianoKeyFrequencies pianoKeyFrequencies;

    SixthOrderBandPassFilter audioFilter; //Bandpass filter

    double minAmplitude = 0.001;
    double harmonics[NOTES_ON_PIANO][10] = {0.0};

    long long sumTotalFL = 0;
    long long sumTotalFLOld = 0;

    double centsOffsetAvgPlot[NOTES_ON_PIANO] = {0.0};
    TuningPartials tuningPartials;
    int targetPartials[MAX_PARTIALS] = {0};
    double centsOffsetFull[MAX_PARTIALS] = {0.0};
    double targetPeakFreq[MAX_PARTIALS] = {0.0};
    double measuredPeakFreq[MAX_PARTIALS] = {0.0};
    int targetPeakFreqLen;
    double measuredPeakFreqOld[MAX_PARTIALS] = {0.0};

    double delta[NOTES_ON_PIANO] = {
            -36.6349775557606,
            -33.1257198762569,
            -29.9262431846599,
            -27.0450796162004,
            -24.2619890252974,
            -21.6611223086803,
            -19.5223058760959,
            -17.4736056544944,
            -15.6801046818303,
            -14.1033352502120,
            -12.6903268259606,
            -11.5193043372188,
            -10.3155834544853,
            -9.46150767341101,
            -8.67689301422420,
            -7.98835880950561,
            -7.38802547979263,
            -6.77953061654821,
            -6.35500616206642,
            -5.93393825038139,
            -5.57074357293662,
            -5.21972652269078,
            -4.86286471429269,
            -4.60426881202060,
            -4.27757503002189,
            -4.07785636805046,
            -3.85437997223431,
            -3.64131258926335,
            -3.45434538683666,
            -3.21464888948233,
            -3.12082684285463,
            -2.95877053744443,
            -2.86870218628016,
            -2.74692055556947,
            -2.59221550974313,
            -2.47347874856883,
            -2.25809793289941,
            -2.15761946826088,
            -1.99188284587061,
            -1.81733761994875,
            -1.61840234643082,
            -1.36266431183000,
            -1.24331212079753,
            -1.02550602276600,
            -0.846259058398925,
            -0.666268328276725,
            -0.468032468843665,
            -0.287632171530529,
            0,
            0.0910913221861431,
            0.259680705735477,
            0.369745399014265,
            0.582194090195731,
            0.895405566536889,
            1.11935097265095,
            1.45104472695485,
            1.71966124302188,
            2.05166693758714,
            2.44530948574240,
            2.81762569464124,
            3.28419653169139,
            3.66410406453968,
            4.18174213847875,
            4.78062332471958,
            5.29060087071467,
            5.82474273634808,
            6.32553097764919,
            6.91382426515146,
            7.29182453202180,
            7.71561216501326,
            8.04084995634415,
            8.74781786644685,
            9.67473597524058,
            10.5569623638061,
            11.6049503661573,
            12.6686121521041,
            13.8482362834569,
            15.17355,
            16.57153,
            18.16634,
            19.78581,
            21.67102,
            23.81153,
            25.94373,
            28.23564,
            30.65342,
            33.38221,
            36.77633
    };
    double Bave[NOTES_ON_PIANO] = {0.0};  // 0-based

    double freqRange[INTERNAL_FRAME_LEN] = {0.0};
    double freqRangeToneDetectorMap[4600] = {0.0};
    float sampleRate = 16000.0f;

    double num;
    bool spinnerEnabled[4];
    double angle;
    bool offsetOver;
    float m_fftResArray[INTERNAL_FRAME_LEN / 2] = {0};
    unsigned int fftBits = 12;
    unsigned int fftSize = 1 << fftBits; // 2^12 = 4096

    int countertc;
    double inharmonicity[NOTES_ON_PIANO][3] = {0.0};  // 0-based
    double inharmonicityWeightMultiplier;
    double centsCorrection[NOTES_ON_PIANO][16] = {0.0};
    double peakHeightsGuess[NOTES_ON_PIANO][16] = {0.0};
    double peakHeights[NOTES_ON_PIANO][16] = {0.0};
    std::array<int, MAX_PARTIALS> goodPeaksIndex;

    double bxfitMin[BXFIT_SIZE] = {0.0001, -0.25, 0.005, 0.0633};  // Lower boundaries
    double bxfitMax[BXFIT_SIZE] = {0.002, -0.01, 0.0395, 0.115};   // upper boundaries
    // initial "guess" values for bxfit
    double bxfitDefaultGuess[BXFIT_SIZE] = {0.0007, -0.1055, 0.0214, 0.0892};
    double bxfit[BXFIT_SIZE] = {0.0};
    double temperament[TEMPERAMENT_SIZE] = {0.0};

    int bxBuffer[NOTES_ON_PIANO] = {0};
    double byBuffer[NOTES_ON_PIANO] = {0.0};
    int bxLen = 0;

    int dataSumTrimDiffLPF = 1;

    bool isBxFitSet;
    bool isTargetLengthCount = false;
    double centsAve[NOTES_ON_PIANO][16] = {0.0};
    double peakHeightsSmooth[NOTES_ON_PIANO][16] = {0.0};
    double centsOffsetAvg = 0;
    volatile double centsOffsetAvgLPF;

    double centsOffsetAvgXMem1, centsOffsetAvgXMem2;

    short ringBuffer[RING_BUFFER_LEN] = {0};
    unsigned long long ringBufferWriteCounter = 0;
    unsigned long long ringBufferFastLoopReadCounter = 0;
    unsigned long long ringBufferZeroCrossingReadCounter = 0;
    short overlapBuffer[INTERNAL_FRAME_LEN]  = {0};
    std::mutex bufferMutex, calculateBxAndTxMutex;
    int fastLoopMode = 0;

    bool isPitchRaiseMeasurementOnFlag = false;
    int pitchRaiseKeys[12] = {0};

    double calibrationFactor = 1.0;

    double inputSamplingRate;
    double inputSamplingRatio;
    SamplingRateConverter *srConverter;

    // Zero crossing for spinner and cents offset
    static constexpr int ZERO_CROSSING_FRAME_LEN = 512;
    static constexpr int PHASE_QUEUE_SIZE = 4;
    static constexpr float SPINNER_INACTIVE = -2.0F;
    static constexpr float SPINNER_EMPTY = -1.0F;
    float currentNoteZC;
    double currentFreqZC;
    std::array<double, MAX_PARTIALS> cyclesPerSample;
    std::array<double, MAX_PARTIALS> referencePhase;
    std::array<float, MAX_PARTIALS> lastZeroCrossingX;
    BandPassFilter bpf[MAX_PARTIALS][2];

    float targetFreqGeometricMean[NOTES_ON_PIANO] = {0.0};
    float targetFreqBandwidth[NOTES_ON_PIANO] = {0.0};
    BandPassFilter wideBpf;

    float dataSumOld;
    float avgAmp;
    std::array<float, 2> filteredData[MAX_PARTIALS];
    std::array<float, 3> centsOffsetZC[MAX_PARTIALS];
    std::array<double, NOTES_ON_PIANO> overpullCents;
    float centsOffsetZCFiltered[MAX_PARTIALS] = {0.0};
    std::vector<float> centsOffsetZCAve;
    float centsOffsetCombined;
    bool qualityTestCombined;
    std::array<std::vector<float>, MAX_PARTIALS> phaseData;
    std::array<std::vector<float>, MAX_PARTIALS> alphaData;
    std::mutex phaseDataMutex;

    void clearPhase(int partial);

    void addPhase(int partial, float phase, float alpha);

    void setZeroCrossingFreq();

    void clearZeroCrossing();

    void processZeroCrossingBuffer(const short *buffer, const int length);
};

#endif /* ToneDetector_h */
