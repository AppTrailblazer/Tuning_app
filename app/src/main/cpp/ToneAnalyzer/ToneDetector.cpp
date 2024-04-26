#include "ToneDetector.h"
#include "MathUtils.h"
#include "debug_util.h"
#include "IntervalWidths.h"
#include "IntervalWeights.h"
#include <iostream>
#include <string>
#include <vector>
#include <cfloat>

#define MINUS_150_CENTS 0.9170040432046712F
#define PLUS_100_CENTS 1.0594630943592953F
#define INV_SHORT_MAX 0.00003052F

static inline float cpx_abs(kiss_fft_cpx f) {
    return sqrtf(f.r * f.r + f.i * f.i);
}

ToneDetector::ToneDetector() {
    srConverter = new SamplingRateConverter();
    inputSamplingRate = 16000.0;
    inputSamplingRatio = 1.0;

    cfgFastLoop = kiss_fftr_alloc(INTERNAL_FRAME_LEN, 0, 0, 0);
    cfgNoteDetector = kiss_fftr_alloc(fftSize, 0, 0, 0);
    cfgXcorr2 = kiss_fftr_alloc(fftSize * 2, 0, 0, 0);
    cfgXcorri2 = kiss_fftr_alloc(fftSize * 2, 1, 0, 0);
    cfgXcorr = kiss_fftr_alloc(4096, 0, 0, 0);
    cfgXcorri = kiss_fftr_alloc(4096, 1, 0, 0);
    noteChangeMode = NOTE_CHANGE_AUTO;

    countertc = 0;

    currentNote = 49;
    previousNoteF = 49;
    currentNoteZC = -1;
    currentFreqZC = 0.0F;

    overpullCents.fill(0.0);

    preparation();
}

void ToneDetector::setInputSamplingRate(double samplingRate) {
    inputSamplingRate = samplingRate;
    inputSamplingRatio = 16000.0 / samplingRate;
    audioFilter.setSamplingRate(samplingRate); // Set sample rate for bandpass filter
    LOGI("sr:%f srratio:%f \n", inputSamplingRate, inputSamplingRatio);
}


int ToneDetector::getFx(double *fx) {
    memcpy(fx, centsOffsetAvgPlot, sizeof(double) * NOTES_ON_PIANO);
    return NOTES_ON_PIANO;
}

void ToneDetector::setPitchOffsetFactor(double factor) {
    pianoKeyFrequencies.setOffsetFactor(factor);

    performPeakHeightsSmoothing();

    // Convert the peak heights to a new scale related to loudness
    double peakHeightsCorrected[NOTES_ON_PIANO][16] = {0.0};
    // call a method that calculate peakHeightsCorrected based on L values
    calculatePeakHeightsCorrected(peakHeightsSmooth, peakHeightsCorrected);
    // call tcCalculator function to get delta values.
    tcCalculatorP(centsAve, peakHeightsCorrected, delta);

    pianoKeyFrequencies.computeTargetFrequencies(delta, Bave);
    memset(centsOffsetAvgPlot, 0, sizeof(centsOffsetAvgPlot));
    calculateCentsOffsetAvgPlotArray();
}

void ToneDetector::setCalibrationFactor(double factor) {
    calibrationFactor = factor;
    sampleRate = calibrationFactor * 16000.0F;

    // Frequency range for FFT of a sample length of nPower2
    for (int i = 0; i < INTERNAL_FRAME_LEN; i++) {
        freqRange[i] = (float) i * sampleRate / INTERNAL_FRAME_LEN;
    }
    // Create an array that maps the frequencies from 1 to 4,500 to the
    // nearest index of freqRangeToneDetector

    double ratio = INTERNAL_FRAME_LEN / sampleRate;

    for (int n = 0; n < 4600; n++) {
        freqRangeToneDetectorMap[n] = round(((double) n + 1.0) * ratio) + 1.0;
    }

    double freqRangeDiff[4095];
    calculateDiff(freqRange, freqRangeDiff, 4095);

}

void ToneDetector::setInharmonicityWeight(double inharmonicityWeight) {
    inharmonicityWeightMultiplier =
            -1.0 / sqrt(10.0) * inharmonicityWeight / (inharmonicityWeight - 1.0);
}


void ToneDetector::preparation() {
    for (int i = 0; i < BXFIT_SIZE; i++) {
        bxfit[i] = bxfitDefaultGuess[i];
    }
    isBxFitSet = false;
    memset(Bave, 0, sizeof(double) * NOTES_ON_PIANO);
    memset(bxBuffer, 0, sizeof(int) * NOTES_ON_PIANO);
    memset(byBuffer, 0, sizeof(double) * NOTES_ON_PIANO);
    bxLen = 0;

    for (int i = 0; i < NOTES_ON_PIANO; i++) {
        Bave[i] = bxfit[0] * exp(bxfit[1] * (i + 1)) + bxfit[2] * exp(bxfit[3] * (i - 87));
    }

    for (int i = 0; i < NOTES_ON_PIANO; i++) {
        memset(centsCorrection[i], 0, sizeof(double) * 16);
        memset(peakHeights[i], 0, sizeof(double) * 16);
        memset(centsAve[i], 0, sizeof(double) * 16);
    }

    memset(centsOffsetFull, 0, sizeof(double) * 5);
    for (int n = 0; n < 10; n++) {
        for (int i = 0; i < NOTES_ON_PIANO; i++) {
            harmonics[i][n] = 0;
        }
    }

    dataSumTrimDiffLPF = 1.0;

    for (int n = 0; n < 16; n++) {
        int nn = n + 1;
        for (int i = 0; i < NOTES_ON_PIANO; i++) {
            centsAve[i][n] = (600 * log2((1 + (nn * nn) * Bave[i]) / (1 + Bave[i])));
        }
    }

    // init freqRange
    for (int i = 0; i < 4096; i++) {
        freqRange[i] = i * (sampleRate / 4096.0F);  // fill frequency range array
    }
    double freqRangeDiff[4095] = {0.0};
    calculateDiff(freqRange, freqRangeDiff, 4095);

    sumTotalFLOld = 0;

    // init peakHeights
    for (int j = 0; j < NOTES_ON_PIANO; j++) {
        peakHeightsGuess[j][0] = (1.0 / (1.0 + exp(-0.1833 * (j + 1 - 37))));
        peakHeightsGuess[j][1] = (10 * exp(-pow(j + 1 - 37, 2) / pow(20, 2))) / 50.0;
        peakHeightsGuess[j][2] = (10 * exp(-pow(j + 1 - 35, 2) / pow(20, 2))) / 50.0;
        peakHeightsGuess[j][3] = (10 * exp(-pow(j + 1 - 33, 2) / pow(20, 2))) / 50.0;
        peakHeightsGuess[j][4] = (10 * exp(-pow(j + 1 - 27, 2) / pow(20, 2))) / 50.0;
        peakHeightsGuess[j][5] = (10 * exp(-pow(j + 1 - 20, 2) / pow(20, 2))) / 50.0;
        peakHeightsGuess[j][6] = (10 * exp(-pow(j + 1 - 12, 2) / pow(20, 2))) / 50.0;
        peakHeightsGuess[j][7] = (10 * exp(-pow(j + 1 - 7, 2) / pow(20, 2))) / 50.0;
        peakHeightsGuess[j][8] = (10 * exp(-pow(j + 1 - 4, 2) / pow(20, 2))) / 50.0;
        peakHeightsGuess[j][9] = (10 * exp(-pow(j + 1 - 1, 2) / pow(20, 2))) / 50.0;

        peakHeights[j][0] = peakHeightsGuess[j][0];
        peakHeights[j][1] = peakHeightsGuess[j][1];
        peakHeights[j][2] = peakHeightsGuess[j][2];
        peakHeights[j][3] = peakHeightsGuess[j][3];
        peakHeights[j][4] = peakHeightsGuess[j][4];
        peakHeights[j][5] = peakHeightsGuess[j][5];
        peakHeights[j][6] = peakHeightsGuess[j][6];
        peakHeights[j][7] = peakHeightsGuess[j][7];
        peakHeights[j][8] = peakHeightsGuess[j][8];
        peakHeights[j][9] = peakHeightsGuess[j][9];
    }

    memset(ringBuffer, 0, sizeof(short) * RING_BUFFER_LEN);
    memset(overlapBuffer, 0, sizeof(short) * INTERNAL_FRAME_LEN);

    int pTA = fftSize;
    //double out[]=new double[pTA];
    float start = -(pTA / 2 - 0.5F);
    for (int i = 0; i < pTA; i++, start++) {
        float a = (-(start * start)) / (2 * (pTA / 7.0F) * (pTA / 7.0F));
        window[i] = expf(a) * INV_SHORT_MAX;
    }

    targetPeakFreqLen = 0;
    for (int partial = 0; partial < MAX_PARTIALS; partial++) {
        if (tuningPartials.get(partial, currentNote - 1) != 0) {
            targetPartials[targetPeakFreqLen] = tuningPartials.get(partial, currentNote - 1) - 1;
            targetPeakFreq[targetPeakFreqLen] = pianoKeyFrequencies.targetFrequency(
                    targetPartials[targetPeakFreqLen], currentNote - 1);
            targetPeakFreqLen++;
        }
        measuredPeakFreq[partial] = 0;
        measuredPeakHeight[partial] = 0;
        measuredPeakFreqOld[partial] = 0;
    }

    centsOffsetAvgXMem1 = centsOffsetAvgXMem2 = 0.0;

    // Frequency range for FFT of a sample length of nPower2
    for (int i = 0; i < INTERNAL_FRAME_LEN; i++) {
        freqRange[i] = i * sampleRate / INTERNAL_FRAME_LEN;
    }
    // Create an array that maps the frequencies from 1 to 4,500 to the
    // nearest index of freqRangeToneDetector

    float ratio = INTERNAL_FRAME_LEN / sampleRate;

    for (int n = 0; n < 4600; n++) {
        freqRangeToneDetectorMap[n] = roundf((n + 1.0F) * ratio) + 1.0F;
    }

    for (int i = 0; i < NOTES_ON_PIANO; i++) {
        for (int k = 0; k < 3; k++) {
            inharmonicity[i][k] = 0;
        }
    }

    performPeakHeightsSmoothing();
    // Convert the peak heights to a new scale related to loudness
    double peakHeightsCorrected[NOTES_ON_PIANO][16] = {0.0};
    // call a method that calculate peakHeightsCorrected based on L values
    calculatePeakHeightsCorrected(peakHeightsSmooth, peakHeightsCorrected);
    // call tcCalculator function to get delta values.
    tcCalculatorP(centsAve, peakHeightsCorrected, delta);

    pianoKeyFrequencies.computeTargetFrequencies(delta, Bave);

    memset(centsOffsetAvgPlot, 0, sizeof(double) * NOTES_ON_PIANO);
    centsOffsetAvgLPF = 0;

    referencePhase.fill(0);
    lastZeroCrossingX.fill(-1.0F);
    avgAmp = 0.0F;

    for (int note = 0; note < NOTES_ON_PIANO; note++) {
        float sumlog = 0.0F, minFreq = FLT_MAX, maxFreq = 0.0F;
        int count = 0;
        for (int partial = 0; partial < MAX_PARTIALS; partial++) {
            if (tuningPartials.get(partial, note) != 0) {
                float freq = pianoKeyFrequencies.targetFrequency(
                        tuningPartials.get(partial, note) - 1, note);
                sumlog += logf(freq);
                if (freq < minFreq) {
                    minFreq = freq;
                }
                if (freq > maxFreq) {
                    maxFreq = freq;
                }
                count++;
            }
        }
        targetFreqGeometricMean[note] = expf(sumlog / count);
        targetFreqBandwidth[note] = 2 + MathUtils::log2(maxFreq / minFreq);
    }
    clearZeroCrossing();
    setZeroCrossingFreq();
}

#if 1

ToneDetector::~ToneDetector() {
    delete srConverter;
    kiss_fftr_free(cfgFastLoop);
    kiss_fftr_free(cfgNoteDetector);
    kiss_fftr_free(cfgXcorr2);
    kiss_fftr_free(cfgXcorri2);
    kiss_fftr_free(cfgXcorr);
    kiss_fftr_free(cfgXcorri);
}

int ToneDetector::getBx(int *bx) {
    memcpy(bx, bxBuffer, sizeof(int) * bxLen);
    return bxLen;
}

int ToneDetector::getBy(double *by) {
    memcpy(by, byBuffer, sizeof(double) * bxLen);
    return bxLen;
}

int ToneDetector::getBxfit(double *bxfitx) {
    memcpy(bxfitx, bxfit, sizeof(double) * BXFIT_SIZE);
    return BXFIT_SIZE;
}

int ToneDetector::getTemperament(double *out) {
    memcpy(out, temperament, sizeof(double) * TEMPERAMENT_SIZE);
    return TEMPERAMENT_SIZE;
}

int ToneDetector::getBave(double *Bave) {
    memcpy(Bave, this->Bave, sizeof(double) * NOTES_ON_PIANO);
    return NOTES_ON_PIANO;
}

int ToneDetector::getDelta(double *deltax) {
    memcpy(deltax, delta, sizeof(double) * NOTES_ON_PIANO);
    return NOTES_ON_PIANO;
}

int ToneDetector::getInharmonicity(double *data) {
    memcpy(data, inharmonicity, sizeof(double) * 3 * NOTES_ON_PIANO);
    return NOTES_ON_PIANO * 3;
}

void ToneDetector::setInharmonicity(const double *data) {
    memcpy(inharmonicity, data, sizeof(double) * 3 * NOTES_ON_PIANO);

    bxLen = 0;
    for (int i = 0; i < NOTES_ON_PIANO; i++) {
        if (inharmonicity[i][0] != 0) {
            bxBuffer[bxLen] = i + 1;
            byBuffer[bxLen] = inharmonicity[i][0];
            bxLen++;
        }
    }
}

int ToneDetector::getPeaksHeight(double *data) {
    memcpy(data, peakHeights, sizeof(double) * 16 * NOTES_ON_PIANO);
    return NOTES_ON_PIANO * 16;
}

void ToneDetector::setPeaksHeight(const double *data) {
    memcpy(peakHeights, data, sizeof(double) * 16 * NOTES_ON_PIANO);
}

void ToneDetector::setBxFit(const double *data) {
    memcpy(bxfit, data, sizeof(double) * BXFIT_SIZE);
}

void ToneDetector::setTemperament(const double *data) {
    memcpy(temperament, data, sizeof(double) * TEMPERAMENT_SIZE);
}

void ToneDetector::setDelta(const double *data) {
    memcpy(delta, data, sizeof(double) * NOTES_ON_PIANO);
}

void ToneDetector::setFx(const double *fx) {
    memcpy(centsOffsetAvgPlot, fx, sizeof(double) * NOTES_ON_PIANO);
}

void ToneDetector::setHarmonics(const double *data) {
    memcpy(harmonics, data, sizeof(double) * 10 * NOTES_ON_PIANO);
}

int ToneDetector::getHarmonics(double *data) {
    memcpy(data, harmonics, sizeof(double) * 10 * NOTES_ON_PIANO);
    return NOTES_ON_PIANO * 10;
}

int ToneDetector::getFFTResArray(float *fftArray) {
    memcpy(fftArray, m_fftResArray, sizeof(float) * INTERNAL_FRAME_LEN / 2);
    return INTERNAL_FRAME_LEN / 2;
}

void ToneDetector::addData(short *buffer, int bufferLen) {
    ASSERT_RANGE(bufferLen, 0, 4096);
    short temp[4096] = {0};
    short *outbuffer;
    int outBufferLen = bufferLen;
    audioFilter.filter(buffer, bufferLen);
    if (inputSamplingRatio != 1.0) {
        LOGI("Processing mode isr:%f blen:%d \n", inputSamplingRatio, bufferLen);

        outBufferLen = srConverter->process(buffer, bufferLen, temp, inputSamplingRatio);
        outbuffer = &temp[0];
    } else {
        outbuffer = buffer;
    }
    {
        std::lock_guard<std::mutex> lock(bufferMutex);
        int ringBufferWriteIndex = ringBufferWriteCounter % RING_BUFFER_LEN;
        if (ringBufferWriteIndex + outBufferLen <= RING_BUFFER_LEN) {
            memcpy(ringBuffer + ringBufferWriteIndex, outbuffer, sizeof(short) * outBufferLen);
        } else {
            int n = RING_BUFFER_LEN - ringBufferWriteIndex;
            memcpy(ringBuffer + ringBufferWriteIndex, outbuffer, sizeof(short) * n);
            memcpy(ringBuffer, outbuffer + n, sizeof(short) * (outBufferLen - n));
        }
        ringBufferWriteCounter += outBufferLen;
    }
}


void ToneDetector::getBuffer(short *buffer, int length, unsigned long long position) {
    int ringBufferReadIndex = position % RING_BUFFER_LEN;
    if (ringBufferReadIndex + length <= RING_BUFFER_LEN) {
        memcpy(buffer, ringBuffer + ringBufferReadIndex, sizeof(short) * length);
    } else {
        int n = RING_BUFFER_LEN - ringBufferReadIndex;
        memcpy(buffer, ringBuffer + ringBufferReadIndex, sizeof(short) * n);
        memcpy(buffer + n, ringBuffer, sizeof(short) * (length - n));
    }
}

void complexConjMultiple(kiss_fft_cpx *data, int length) {
    data[0].r = data[0].r * data[0].r;
    data[0].i = 0.0F;

    for (int i = 1; i < length; i++) {
        data[i].r = data[i].r * data[i].r + data[i].i * data[i].i;
        data[i].i = 0.0F;
    }
}

void ToneDetector::xcorr(float *in, double *res, int length) {

    int fftSize = 2 * length;
    kiss_fft_cpx buf[length + 1];
    float buf2[fftSize];

    // take fft and power spectrum
    if (length == 2048) {
        // 4k FFT
        kiss_fftr(cfgXcorr, in, buf);
        complexConjMultiple(buf, length + 1);
        kiss_fftri(cfgXcorri, buf, buf2);
    } else if (length == 4096) {
        // 8k FFT
        kiss_fftr(cfgXcorr2, in, buf);
        complexConjMultiple(buf, length + 1);
        kiss_fftri(cfgXcorri2, buf, buf2);
    }

    for (int i = 0; i < fftSize / 2 - 1; i++) {
        res[i] = buf2[fftSize / 2 + 1 + i] * 0.5f;
        res[i + fftSize / 2 - 1] = buf2[i] * 0.5f;
    }
    res[fftSize - 2] = buf2[fftSize / 2 - 1] * 0.5f;
}

double ToneDetector::getNum() {
    LOGV("Processing mode %d \n", fastLoopMode);
    return num;
}

void ToneDetector::startPitchRaiseMeasurement(const int *keys) {
    if (keys != NULL) {
        memcpy(pitchRaiseKeys, keys, sizeof(int) * 12);
        isPitchRaiseMeasurementOnFlag = true;
    }
}

void ToneDetector::stopPitchRaiseMeasurement() {
    isPitchRaiseMeasurementOnFlag = false;
}

/**
 * Finds optimal buffer for note detection.
 *
 * The optimal buffer starts at ATTACK_POSITION + WINDOW_SIZE if an attack was found
 * or at the window with the maximum sum of absolute amplitudes.
 */
bool ToneDetector::findOptimalDetectNotesBuffer(const short *buffer, unsigned int bufferLen,
                                                short *outBuffer,
                                                unsigned int outBufferLen) {
    const unsigned int WINDOW_SIZE = 512;
    int dataSumTrim = 0;
    int dataSumTrimDiff = 0;
    double dataSumTrimDiffMax = 0;
    int maxDataSum = 0;
    unsigned int dataSumTrimIndex = 0;
    unsigned int maxDataSumIndex = 0;

    unsigned int dataTrimOffset = WINDOW_SIZE * 2;
    bool attackDetected = false;

    int dataSumTrimOld = 0;
    for (int i = 0; i < WINDOW_SIZE; ++i) {
        dataSumTrimOld += abs(buffer[i]); // Sum of the first window
    }

    unsigned int loopEnd = bufferLen - outBufferLen - dataTrimOffset + 1;
    for (unsigned int i = WINDOW_SIZE; i < loopEnd; i += WINDOW_SIZE) {
        dataSumTrim = 0;
        for (unsigned int dataTrimCounter = i;
             dataTrimCounter < i + WINDOW_SIZE; ++dataTrimCounter) {
            dataSumTrim += abs(buffer[dataTrimCounter]); // Sum Nth window
        }

        dataSumTrimDiff = dataSumTrim - dataSumTrimOld; // Find the change between windows
        dataSumTrimDiffLPF = (9 * dataSumTrimDiffLPF + 1 * abs(dataSumTrimDiff)) /
                             10; // Rolling smooth (low pass filter)

        if (dataSumTrimDiffLPF < 1) {
            dataSumTrimDiffLPF = 1;
        }

        if (dataSumTrimDiff / dataSumTrimDiffLPF > dataSumTrimDiffMax) {
            dataSumTrimDiffMax = dataSumTrimDiff / dataSumTrimDiffLPF;
            dataSumTrimIndex = i;
        }

        if (dataSumTrim > maxDataSum) {
            maxDataSum = dataSumTrim;
            maxDataSumIndex = i;
        }

        dataSumTrimOld = dataSumTrim;
    }
    LOGV("dataSumTrimDiffMax =%f", dataSumTrimDiffMax);
    if (dataSumTrimDiffMax > 2) {
        attackDetected = true;
        int dataTrimIndexToUse = dataSumTrimIndex + dataTrimOffset;
        dataTrimIndexToUse = (int) round(
                dataTrimIndexToUse + (bufferLen - outBufferLen - dataTrimIndexToUse) / 3.0);
        memcpy(outBuffer, buffer + dataTrimIndexToUse, outBufferLen * sizeof(short));
    } else {
        int dataTrimIndexToUse = maxDataSumIndex + dataTrimOffset;
        dataTrimIndexToUse = (int) round(
                dataTrimIndexToUse + (bufferLen - outBufferLen - dataTrimIndexToUse) / 3.0);
        memcpy(outBuffer, buffer + dataTrimIndexToUse, outBufferLen * sizeof(short));
    }

    return attackDetected;
}

bool ToneDetector::audioDataHasPianoKeyAttackAtTheSecondHalf(const short *buffer,
                                                             const unsigned int bufferLen) {
    unsigned long sumLeft = 0;
    unsigned long sumRight = 0;
    for (int i = 0; i < bufferLen / 2; i++) {
        sumLeft += abs(buffer[i]);
        sumRight += abs(buffer[i + bufferLen / 2]);
    }

    return sumLeft * 3 <= sumRight;
}

bool ToneDetector::detectNotes() {
    unsigned int bufferLen = fftSize;
    unsigned int longBufferLen = 3 * fftSize;
    short longBuffer[longBufferLen];
    memset(longBuffer, 0, longBufferLen * sizeof(short));

    {
        std::lock_guard<std::mutex> lock(bufferMutex);
        if (ringBufferWriteCounter < longBufferLen) {
            // not enough data read
            LOGVT("DETECT_NOTES", "Not enough data to perform note detection. Skipping");
            return false;
        }
        // get the latest bufferLen samples
        getBuffer(longBuffer, longBufferLen, ringBufferWriteCounter - longBufferLen);
    }

    // detect silence
    if (isSilence(longBuffer, longBufferLen)) {
        LOGVT("DETECT_NOTES", "Audio data is silence. Skipping");
        return true;
    }

    short buffer[bufferLen];
    memset(buffer, 0, bufferLen * sizeof(short));

    bool hasAttack = findOptimalDetectNotesBuffer(longBuffer, longBufferLen, buffer, bufferLen);

    // log_array("DETECT_NOTES_SHORT_BUFFER", buffer, bufferLen); // print log of entire buffer

    /*
     * Don't measure data that is right when the "attack" happens (when the piano key is struck).
     * The SNR is low there.
     */
    if (audioDataHasPianoKeyAttackAtTheSecondHalf(buffer, bufferLen)) {
        LOGVT("DETECT_NOTES", "Audio data has piano key attack at the second half. Skipping");
        return true;
    }

    float fbuffer[bufferLen];
    memset(fbuffer, 0, bufferLen * sizeof(float));

    // apply windowing
    for (int i = 0; i < bufferLen; i++) {
        fbuffer[i] = buffer[i] * window[i];
    }

    // take fft and power spectrum
    kiss_fft_cpx buf[fftSize / 2 + 1];
    // 4k FFT
    kiss_fftr(cfgNoteDetector, fbuffer, buf);

    // take abs (yDFT)
    const float THRESHOLD = 1000;
    float maxAmplitude = -THRESHOLD - 1;
    for (int i = 0; i < bufferLen / 2; i++) {
        fbuffer[i] = cpx_abs(buf[i]);
        if (std::isnan(fbuffer[i]) || fabs(fbuffer[i]) > THRESHOLD) {
            // to avoid nan or inf values
            fbuffer[i] = 0;
        }

        // record the harmonic with the maximum amplitude
        if (fbuffer[i] > maxAmplitude) {
            maxAmplitude = fbuffer[i];
        }
    }
    memset(fbuffer + (bufferLen / 2), 0, sizeof(float) * bufferLen / 2);
    memcpy(m_fftResArray, fbuffer, sizeof(float) * bufferLen / 2);

    fbuffer[0] = maxAmplitude;

    // log_array("FBUFFER", fbuffer, bufferLen / 2);  //Print the full buffer in the log

    // calculate peakMinSeparation
    int peakMinSeparation = (int) floorf(26.0F / (sampleRate / 4096.0F));
    peakMinSeparation--;

    double fftAutoCor[4096] = {0.0};
    xcorr(fbuffer, fftAutoCor, 2048);

    double subYdft[2048] = {0.0};
    for (int i = 0; i < 2048; i++) {
        // divide data on max amplitude
        subYdft[i] = fbuffer[i] / maxAmplitude;
    }
    double subFreqRange[2048] = {0.0};
    // get first 2048 elements from freqRange
    memcpy(subFreqRange, freqRange, 2048 * sizeof(double));
    double values[MAX_PEAKS] = {0.0};
    double freqs[MAX_PEAKS] = {0.0};
    int indexes[MAX_PEAKS] = {0};
    unsigned int nPeaks = peakFinder.findPeaks(values, freqs, indexes, subFreqRange, subYdft, 0.01,
                                               0.1, true,
                                               2048);

    log_array("PEAK FREQUENCIES", freqs, nPeaks);
    log_array("PEAK AMPLITUDES", values, nPeaks);

    // get elements from freqRange with indices between peakMinSeparation and 2048.
    double subFreqRange2[2048 - peakMinSeparation];
    memcpy(subFreqRange2, freqRange + peakMinSeparation,
           sizeof(double) * (2048 - peakMinSeparation));

    double subFFTAutoCor[4096] = {0.0};
    int start = 2048 + peakMinSeparation - 1;
    int end = 4096 - 1;
    // ignore redundant data
    memcpy(subFFTAutoCor, fftAutoCor + start, (end - start) * sizeof(double));
    // calculate max of auto correlation data
    double maxAmpCor = *std::max_element(subFFTAutoCor, subFFTAutoCor + end - start);
    for (int i = 0; i < end - start; i++) {  // loop over all elements of subFFTAutoCor array
        subFFTAutoCor[i] /= maxAmpCor;  // divide the elements on maxAmpCor
    }

    double acvalues[MAX_PEAKS] = {0.0};
    double acfreqs[MAX_PEAKS] = {0.0};
    int acindexes[MAX_PEAKS] = {0};
    unsigned int acpeaks = peakFinder.findPeaks(acvalues, acfreqs, acindexes, subFreqRange2,
                                                subFFTAutoCor,
                                                0.02, 0.33,
                                                true, end - start);

    LOGV("Number of peaks = %d", nPeaks);
    if (nPeaks != 0) {
        double lowestHarmonic = freqs[0];
        // call a method to get the Index of max value in peaks
        int maxPeakIndex = getMaxIndex(values, nPeaks);
        // get the frequency of max value peak
        double tallestPeak = freqs[maxPeakIndex];

        double peakMedian = lowestHarmonic;
        double meanMedian = peakMedian;
        double lowestDiff = tallestPeak;
        if (nPeaks > 1) {
            lowestDiff = freqs[1] - freqs[0];
        }
        double lowerMedian = 0;

        if (nPeaks > 2) {
            //Median of differences of consecutive peaks (harmonics)
            double peaksDiff[nPeaks - 1];
            memset(peaksDiff, 0, sizeof(double) * (nPeaks - 1));
            // call a method to calculate diff of peaks
            calculateDiff(freqs, peaksDiff, nPeaks);

            if (nPeaks - 1 > 7) {
                peakMedian = MathUtils::median(peaksDiff, 7);
            } else {
                peakMedian = MathUtils::median(peaksDiff, nPeaks - 1);
            }

            int filteredPeakCount = 0;
            double filteredPeakSum = 0;

            for (int i = 0; i < nPeaks - 1; i++) {
                if (peaksDiff[i] > peakMedian / 1.3 && peaksDiff[i] < peakMedian * 1.1) {
                    filteredPeakSum += peaksDiff[i];
                    filteredPeakCount++;
                }
            }

            meanMedian = 0;
            if (filteredPeakCount > 0) {
                meanMedian = filteredPeakSum / filteredPeakCount;
            }

            if (acfreqs[0] / peaksDiff[0] > 0.94 && acfreqs[0] / peaksDiff[0] < 1.4) {
                lowestDiff = peaksDiff[0];
                if (acfreqs[0] / peaksDiff[1] > 0.94 && acfreqs[0] / peaksDiff[1] < 1.4 &&
                    peaksDiff[1] < peaksDiff[0]) {
                    lowestDiff = peaksDiff[1];
                }
            } else if (acfreqs[0] / peaksDiff[1] > 0.94 && acfreqs[0] / peaksDiff[1] < 1.4) {
                lowestDiff = peaksDiff[1];
            }

            if (nPeaks > 10) {
                double lowerDiff[4] = {0.0};
                calculateDiff(freqs, lowerDiff, 4);
                for (int lmc = 0; lmc < 4; lmc++) {
                    if (acfreqs[0] / lowerDiff[lmc] > 0.94 &&
                        acfreqs[0] / lowerDiff[lmc] < 1.4) {
                        continue;
                    } else if (acfreqs[0] / (lowerDiff[lmc] / 2) > 0.94 &&
                               acfreqs[0] / (lowerDiff[lmc] / 2) < 1.4) {
                        lowerDiff[lmc] = lowerDiff[lmc] / 2;
                    } else if (acfreqs[0] / (lowerDiff[lmc] / 3) > 0.94 &&
                               acfreqs[0] / (lowerDiff[lmc] / 3) < 1.4) {
                        lowerDiff[lmc] = lowerDiff[lmc] / 3;
                    } else {
                        lowerDiff[lmc] = 0;
                    }
                }
                lowerMedian = MathUtils::median(lowerDiff, 4);
            }
        }

        /*Find the closest piano key (by index) for various indicators*/
        int lowestTone = closestKeyP12(lowestHarmonic);
        int loudestTone = closestKeyP12(tallestPeak);
        int medianTone = closestKey(peakMedian);
        int meanMedianTone = closestKey(meanMedian);
        if (acpeaks > 0 && acfreqs[0] > 25) {
            acTone = closestKey(acfreqs[0]);
        } else {
            acTone = 100;
        }
        int lowestDiffTone = closestKeyP12(lowestDiff);
        int lowerMedianTone = 0;
        if (lowerMedian != 0 && lowerMedian > 27) {
            lowerMedianTone = closestKey(lowerMedian);
        } else {
            lowerMedianTone = 0;
        }

        //	Correct for if there is a double peak around the loudest tone
        if (abs(lowestTone - loudestTone) == 1) {
            lowestTone = loudestTone;
        }

        //Initially set the candidate note to be the loudest tone (the case for treble)
        candidateNote = loudestTone;

        /*If acTone and meanMedian line up (should be the case for pretty much
         everywhere but the very high treble) make that the candidate note.*/
        if (abs(acTone - meanMedianTone) <= 3) {
            if (meanMedian > 26.0) {   //don't want to go below 27.5 Hz
                candidateNote = static_cast<int>(fmin(acTone, meanMedianTone));
            }
        }

        //correct for if the candidate note is off a bit from the lowest detected peak
        if (acTone == candidateNote - 12) {
            candidateNote = acTone;
        }

        //Correct for effects of inharmonicity
        if (candidateNote <= 30) {
            if (((candidateNote - lowestDiffTone) < 5) && ((candidateNote - lowestDiffTone) > 0)) {
                candidateNote = lowestDiffTone;
            }
        }

        if (abs(candidateNote - lowerMedianTone) < 2 &&
            lowestDiffTone == lowerMedianTone &&
            candidateNote != lowerMedianTone) {
            candidateNote = lowerMedianTone;
        }


        // Final adjustments based on loudestTone
        if (abs(loudestTone - candidateNote) == 1) {
            candidateNote = loudestTone;
        } else if (abs(loudestTone - candidateNote - 12) == 1) {
            candidateNote = loudestTone - 12;
        } else if (abs(loudestTone - candidateNote - 19) == 1) {
            candidateNote = loudestTone - 19;
        } else if (abs(loudestTone - candidateNote - 24) == 1) {
            candidateNote = loudestTone - 24;
        }

        if (loudestTone > 85) {
            candidateNote = loudestTone;
        }

        // Logging
        LOGV("NoteDetection: candidateNote=%d, lowestDiffTone=%d, lowestMedianTone=%d, acTone=%d, medianTone=%d, meanMedianTone=%d, lowestTone=%d, loudestTone=%d\n",
             candidateNote, lowestDiffTone, lowerMedianTone, acTone, medianTone,
             meanMedianTone, lowestTone, loudestTone);

        int Q = 0;
        bool condition1 = candidateNote == lowestDiffTone && candidateNote == lowerMedianTone &&
                          abs(candidateNote - medianTone) < 4 && abs(candidateNote - acTone) < 6;
        bool condition2 = candidateNote == acTone &&
                          (candidateNote == medianTone || candidateNote == lowerMedianTone) && (
                                  candidateNote == loudestTone || candidateNote == lowestTone ||
                                  candidateNote == loudestTone - 12 ||
                                  (candidateNote == lowestDiffTone && loudestTone < 70)
                          );
        bool condition3 = candidateNote > 75 && candidateNote == loudestTone;

        if (condition1 || condition2 || condition3) {
            LOGV("Quality check is true. Q = 1");
            Q = 1;
        }

        //Catch anything that went below zero
        if (candidateNote <= 0 || candidateNote > NOTES_ON_PIANO) {
            LOGW("Candidate note is out of range: %d", candidateNote);
            Q = 0;
        }

        //Logic deciding whether to switch the current note.
        if (currentNote != candidateNote) {
            LOGV("currentNote != candidateNote");
            int oldNote = currentNote;
            if (noteChangeMode == NOTE_CHANGE_AUTO) {
                if (Q == 1 && hasAttack) {
                    previousNote = currentNote;
                    setCurrentNote(candidateNote, false, false);
                }
            } else if (noteChangeMode == NOTE_CHANGE_STEP) {
                // For stepwise motion mode
                if (abs(currentNote - candidateNote) <= 3) {
                    if (Q == 1 && hasAttack) {
                        previousNote = currentNote;
                        setCurrentNote(candidateNote, false, false);
                    }
                }
            } else if (noteChangeMode != NOTE_CHANGE_LOCK) {
                // If it somehow ended up outside the accepted 0-2 range pretend it's "auto"
                if (Q == 1) {
                    previousNote = currentNote;
                    setCurrentNote(candidateNote, false, false);
                }
            }

            if (currentNote == candidateNote && oldNote > 0) {
                centsOffsetAvgPlot[oldNote - 1] = calculateCentsOffsetAvgPlot(oldNote - 1);
            }
        }

        roundedQ = round(Q * 10.0) / 10.0;

        if (Q == 1) {
            LOGV("Q = 1");
            bool desiredActiveKey = false;
            if (isPitchRaiseMeasurementOnFlag) {
                int modNote = (currentNote - 1) % 12;
                if (modNote >= 0 && modNote < 12 && pitchRaiseKeys[modNote]) {
                    desiredActiveKey = true;
                }
            }
            if (currentNote == candidateNote &&
                (!isPitchRaiseMeasurementOnFlag || desiredActiveKey) &&
                isRecalculateTuningOn) {  // If the tuning curve is locked we should not be running bAnalysis
                bAnalysis(fbuffer, freqs, values, nPeaks);  // call bAnalysis function
            }
            int n = currentNote - 1;
            {
                double sum = 0;
                for (int i = 0; i < 10; i++) {
                    sum += harmonics[n][i];
                }
                if (sum > 0) {
                    centsOffsetAvgPlot[n] = calculateCentsOffsetAvgPlot(n);
                }
            }
        }
    }
    return true;
}

void ToneDetector::getSpinnerEnabled(bool *enabled) {
    memcpy(enabled, spinnerEnabled, sizeof(spinnerEnabled));
}

void ToneDetector::setCurrentNote(int note, bool relative, bool fromUser) {
    int oldNote = currentNote;
    if (relative) {
        if (currentNote + note < 1) {
            currentNote = 1;
            return;
        }
        if (currentNote + note > NOTES_ON_PIANO) {
            currentNote = NOTES_ON_PIANO;
            return;
        }
        currentNote += note;
    } else {
        ASSERT_RANGE(note, 1, NOTES_ON_PIANO);
        currentNote = note;
        if (currentNote > NOTES_ON_PIANO) {
            LOGW("Trying to set current note to be higher than top bound. Fixing it");
            currentNote = NOTES_ON_PIANO;
        }
        if (currentNote <= 0) {
            LOGW("Trying to set current note to be higher than bottom bound. Fixing it");
            currentNote = 0;
        }
    }
    if (oldNote != currentNote) {
        LOGD("Current note changed from %d to %d", oldNote, currentNote);
#ifdef USE_NSC
        if (!fromUser) {
            // Note switched automatically, increment note switch counter
            NSC++;
        }
#endif
    }
}

void ToneDetector::setNSC(int value) {
    NSC = value;
}

int ToneDetector::getTargetPeakFreq(double *freq) {
    memcpy(freq, targetPeakFreq, targetPeakFreqLen * sizeof(double));
    return targetPeakFreqLen;
}

void ToneDetector::reset() {
    countertc = 0;
    previousNote = 0;
    currentNote = 49; //%Set the current note to be A4 by default
    previousNoteF = 49;
    pianoKeyFrequencies.setOffsetFactor(1.0);

    preparation();
}

void ToneDetector::calculateBxAndTc(bool force) {
    // this can be called from the main thread (forceRecalculate) or detectNotes thread
    std::lock_guard<std::mutex> lock(calculateBxAndTxMutex);

    int bx[NOTES_ON_PIANO] = {0};
    double by[NOTES_ON_PIANO] = {0.0};
    /**
     * inharmonicity(:,1)~=0,2**/
    double inharmon1[NOTES_ON_PIANO] = {0.0};
    /**
     * inharmonicity(:,1)~=0,3**/
    double inharmon2[NOTES_ON_PIANO] = {0.0};

    int ind = 0;
    for (int i = 0; i < NOTES_ON_PIANO; i++) {
        if (inharmonicity[i][0] != 0) {
            bx[ind] = i + 1;
            by[ind] = inharmonicity[i][0];
            inharmon1[ind] = inharmonicity[i][1];
            inharmon2[ind] = inharmonicity[i][2];
            ind++;
        }
    }
    bxLen = ind;

    memcpy(bxBuffer, bx, NOTES_ON_PIANO * sizeof(int));
    memcpy(byBuffer, by, NOTES_ON_PIANO * sizeof(double));
    if (isRecalculateTuningOn) {
        //Run the following only twice per note (on the 1st and 5th measurements)
        if (inharmonicity[currentNote - 1][2] == 1 || inharmonicity[currentNote - 1][2] == 5 ||
            inharmonicity[currentNote - 1][2] == 9 || force) {
            // Procedure to calculate an initial linear guess for the non-linear fit.
            // Hopefully this perturbation will help avoid us getting stuck in local minima
            double bxfitGuess[BXFIT_SIZE];
            int bxLt22Count = 0, bxGt37Index = 0, bxGt37Count = 0;
            double bxGuess[bxLen + 2];     // [  0, bx,  89]
            memset(bxGuess, 0, sizeof(double) * (bxLen + 2));
            double byLogGuess[bxLen + 2];  // [___, by, ___]
            memset(byLogGuess, 0, sizeof(double) * (bxLen + 2));
            bxGuess[0] = 0;
            bxGuess[bxLen + 1] = 89;

            for (int i = 0; i < bxLen; i++) {
                bxGuess[i + 1] = (double) bx[i];
                byLogGuess[i + 1] = log(by[i]);
                if (bx[i] < 22) {
                    bxLt22Count++;
                } else if (i < bxLen - 1 && bx[i] <= 37 && bx[i + 1] > 37) {
                    bxGt37Index = i + 2;
                } else if (bx[i] > 37) {
                    bxGt37Count++;
                }
            }
            if (bxLt22Count >= 3) {  // Bass bridge
                double bassFitCoef[2];
                // Linear fit on semilog scale
                MathUtils::polyFit(bxGuess + 1, bxLt22Count, byLogGuess + 1, 1, bassFitCoef);
                // Make sure first parameter is within range
                if (exp(bassFitCoef[1]) > bxfitMax[0]) {
                    // Fit again with extra fake data point for note "zero"
                    byLogGuess[0] = log(bxfitMax[0]);
                    MathUtils::polyFit(bxGuess, bxLt22Count + 1, byLogGuess, 1, bassFitCoef);
                } else if (exp(bassFitCoef[1]) < bxfitMin[0]) {
                    byLogGuess[0] = log(bxfitMin[0]);
                    MathUtils::polyFit(bxGuess, bxLt22Count + 1, byLogGuess, 1, bassFitCoef);
                }
                // Make sure second parameter (slope) is within range
                if (bassFitCoef[0] > bxfitMax[1] || bassFitCoef[0] < bxfitMin[1]) {
                    byLogGuess[0] = log(bxfitDefaultGuess[0]);
                    MathUtils::polyFit(bxGuess, bxLt22Count + 1, byLogGuess, 1, bassFitCoef);
                }
                // Transform out of log space
                bxfitGuess[0] = exp(bassFitCoef[1]);
                bxfitGuess[1] = bassFitCoef[0];
                // Make sure everything is now within range
                bxfitGuess[0] = std::min(std::max(bxfitGuess[0], bxfitMin[0] * 1.001),
                                         bxfitMax[0] * 0.999);
                // Switch multipliers because slope is negative
                bxfitGuess[1] = std::min(std::max(bxfitGuess[1], bxfitMin[1] * 0.999),
                                         bxfitMax[1] * 1.001);
            } else {
                // Otherwise just use the last values of bxfit for a guess
                bxfitGuess[0] = bxfit[0];
                bxfitGuess[1] = bxfit[1];
            }
            if (bxGt37Count > 3) {
                for (int i = 0; i < bxLen + 2; i++) {
                    bxGuess[i] -= 88;
                }
                double trebFitCoef[2];
                MathUtils::polyFit(bxGuess + bxGt37Index, bxGt37Count, byLogGuess + bxGt37Index, 1,
                                   trebFitCoef);
                if (exp(trebFitCoef[1]) > bxfitMax[2]) {
                    byLogGuess[bxLen + 1] = log(0.5 * (bxfitMax[2] + bxfitDefaultGuess[2]));
                    MathUtils::polyFit(bxGuess + bxGt37Index, bxGt37Count + 1,
                                       byLogGuess + bxGt37Index, 1, trebFitCoef);
                } else if (exp(trebFitCoef[1]) < bxfitMin[2]) {
                    byLogGuess[bxLen + 1] = log(0.5 * (bxfitMin[2] + bxfitDefaultGuess[2]));
                    MathUtils::polyFit(bxGuess + bxGt37Index, bxGt37Count + 1,
                                       byLogGuess + bxGt37Index, 1, trebFitCoef);
                }
                if (trebFitCoef[0] > bxfitMax[3] || trebFitCoef[0] < bxfitMin[3]) {
                    byLogGuess[bxLen + 1] = log(bxfitDefaultGuess[2]);
                    MathUtils::polyFit(bxGuess + bxGt37Index, bxGt37Count + 1,
                                       byLogGuess + bxGt37Index, 1, trebFitCoef);
                }
                bxfitGuess[2] = exp(trebFitCoef[1]);
                bxfitGuess[3] = trebFitCoef[0];
                bxfitGuess[2] = std::min(std::max(bxfitGuess[2], bxfitMin[2] * 1.001),
                                         bxfitMax[2] * 0.999);
                bxfitGuess[3] = std::min(std::max(bxfitGuess[3], bxfitMin[3] * 1.001),
                                         bxfitMax[3] * 0.999);
            } else {
                bxfitGuess[2] = bxfit[2];
                bxfitGuess[3] = bxfit[3];
            }
            //Fit inharmonicity data to a sum of exponentials model
            if (bxLen > 1) {
                double x0[BXFIT_SIZE] = {0.0};
                memcpy(x0, bxfitGuess, sizeof(double) * BXFIT_SIZE);
                double bxfitProposed[NOTES_ON_PIANO] = {0.0};
                // call fMinSearchBnd function, it is a bounded filter function..

                MathUtils::fMinSearchBnd2(x0, bxfitMin, bxfitMax, BXFIT_SIZE, bx, by, bxLen,
                                          inharmon1,
                                          inharmon2, bxfitProposed, NOTES_ON_PIANO);
                FSumSquareFunction function(bx, by, inharmon1, inharmon2, bxLen);

                if (!isBxFitSet || function.value(bxfitProposed) < function.value(bxfit)) {
                    memcpy(bxfit, bxfitProposed, sizeof(double) * BXFIT_SIZE);
                    isBxFitSet = true;
                }
            }

            LOGI("INHARMONICITY: Calc tuning curve iharm %f rsq: %f ctc: %d\n",
                 inharmonicity[currentNote - 1][2], rsq, countertc);

            runTcCalculator(false);
        }
    }
}

// bAnalysis cleans the detected peaks from the FFT spectrum, matches them to note numbers,
// calculates the inharmonicity ("B"), and stores the inharmonicity and peakHeights.
void ToneDetector::bAnalysis(float *data, const double *peakFreqs, const double *peakValues,
                             unsigned int nPeaks) {
    if (currentNote < 1) {
        LOGW("bAnalysis: Current note is less than 1");
        return;
    }

    if (nPeaks < MIN_PEAKS_REQUIRED_FOR_NOTE_ANALYSIS[currentNote - 1]) {
        return;
    }

    double xFit[MAX_PEAKS] = {0.0};
    double yFit[MAX_PEAKS] = {0.0};
    double peaksValues[MAX_PEAKS] = {0.0};
    int peaksIndices[MAX_PEAKS] = {0};
    int indCount = 0;

    // initialize refine peak data
    if (currentNote <= 76) {
        for (int n = 0; n < 10; ++n) {
            std::vector<double> peakFreqsDiff(nPeaks);
            for (int i = 0; i < nPeaks; ++i) {
                peakFreqsDiff[i] = abs(
                        peakFreqs[i] - pianoKeyFrequencies.targetFrequency(n, currentNote - 1));
            }
            unsigned int minElementIndex =
                    std::min_element(peakFreqsDiff.begin(), peakFreqsDiff.end()) -
                    peakFreqsDiff.begin();
            double peakFreqToTargetFreq =
                    peakFreqs[minElementIndex] /
                    pianoKeyFrequencies.targetFrequency(n, currentNote - 1);
            if (peakFreqToTargetFreq > 0.95 && peakFreqToTargetFreq < 1.03) {
                xFit[indCount] = n + 1;
                yFit[indCount] = peakFreqs[minElementIndex];
                peaksIndices[minElementIndex] = 1;
                peaksValues[indCount] = peakValues[minElementIndex];
                indCount++;
            }
        }
    } else {
        LOGI("bAnalysis: CurrentNote is > 76. There's only one harmonics here");
        int maxPeak = -1;
        double maxValue = -100000;
        for (int i = 0; i < nPeaks; i++) {
            // THIS SHOULD PROBABLY HAVE AN EXTRA CONDITION MAKING SURE THE DETECTED PEAK FREQUENCY IS WITHIN THE RANGE OF THE CURRENT NOTE.
            if (peakValues[i] > maxValue) {
                maxValue = peakValues[i];
                maxPeak = i;
            }
        }
        indCount = 1;
        xFit[0] = 1;
        yFit[0] = peakFreqs[maxPeak];
    }
    // Transform arrays for a linear fit to the inharmonicity equation
    double xFitT[indCount];
    double yFitT[indCount];
    memset(xFitT, 0, sizeof(double) * indCount);
    memset(yFitT, 0, sizeof(double) * indCount);
    for (int i = 0; i < indCount; i++) {
        yFitT[i] = pow((yFit[i] / xFit[i]), 2);
        xFitT[i] = xFit[i] * xFit[i];
    }

    // Linear fit
    double f1 = 0;
    double f0 = 0;
    double B = 0;
    if (indCount >= 2) {
        double bFitCoef[2] = {0.0};
        // call a method that calculate a poly fit
        MathUtils::polyFit(xFitT, indCount, yFitT, 1, bFitCoef);

        f0 = sqrtf(bFitCoef[1]); // Theoretical "fundamental" frequency
        B = bFitCoef[0] / bFitCoef[1];  // "B" is the inharmonicity
        f1 = f0 * sqrtf(1 + B); // Freq of 1st harmonic

        //Check the quality value of the fit with an R^2
        double values[indCount];
        memset(values, 0, sizeof(double) * indCount);
        // call a method that calculate a poly value
        MathUtils::polyVal(bFitCoef, 2, xFitT, indCount, values);
        double sum = 0;
        for (int i = 0; i < indCount; i++) {
            double cur = yFitT[i] - values[i];
            sum += cur * cur;
        }
        double var = MathUtils::variance(yFitT, indCount);
        double m = (indCount - 1) * var;
        rsq = 1 - sum / m;

        // New code to see if we can improve the fit by removing one of the data points
        if (rsq < 0.9 && indCount > 3) {
            double max = 0.0;
            int badFitIndex = 0;
            for (int i = 0; i < indCount; i++) {
                double e = fabs(yFitT[i] - (xFitT[i] * bFitCoef[0] + bFitCoef[1]));
                if (e > max) {
                    max = e;
                    badFitIndex = i;
                }
            }
            int indCount2 = indCount - 1;
            double xFitT2[indCount2];
            double yFitT2[indCount2];
            memset(xFitT2, 0, sizeof(double) * indCount2);
            memset(yFitT2, 0, sizeof(double) * indCount2);
            int j = 0;
            for (int i = 0; i < indCount; i++) {
                if (i != badFitIndex) {
                    xFitT2[j] = xFitT[i];
                    yFitT2[j] = yFitT[i];
                    j++;
                }
            }

            // Try the fit again without the worst data point
            MathUtils::polyFit(xFitT2, indCount2, yFitT2, 1, bFitCoef);

            f0 = sqrtf(bFitCoef[1]);
            B = bFitCoef[0] / bFitCoef[1];
            f1 = f0 * sqrtf(1 + B);

            double values[indCount2];
            memset(values, 0, sizeof(double) * indCount2);
            MathUtils::polyVal(bFitCoef, 2, xFitT2, indCount2, values);
            double sum = 0;
            for (int i = 0; i < indCount2; i++) {
                double cur = yFitT2[i] - values[i];
                sum += cur * cur;
            }
            double var = MathUtils::variance(yFitT2, indCount2);
            double m = (indCount2 - 1) * var;
            rsq = 1 - sum / m;
        }

    }

    if (rsq <= 0.9
        || B <= B_LOWER_LIMIT[currentNote - 1]
        || B >= B_UPPER_LIMIT[currentNote - 1]
        || currentNote != closestKeyP12(f1)
        || indCount < MIN_PEAKS_REQUIRED_FOR_NOTE_ANALYSIS[currentNote - 1] * 0.75) {
        return;
    }

    LOGVT("BANALYSIS", "B = %f", B);

    //Store inharmonicity data
    if (inharmonicity[currentNote - 1][0] == 0) {  // case for no previously stored data
        //the inharmonicity itself
        inharmonicity[currentNote - 1][0] = B;
        // The number of peaks used in B calculation
        inharmonicity[currentNote - 1][1] = indCount;
        LOGD("IHARM UPDATE 1 iharm: %f cn: %d\n", inharmonicity[currentNote - 1][2],
             currentNote);
    } else {    //case for existing data (weighted average with new data)
        inharmonicity[currentNote - 1][0] =
                (inharmonicity[currentNote - 1][0] * inharmonicity[currentNote - 1][2] +
                 B) / (inharmonicity[currentNote - 1][2] + 1); //weighted average
        inharmonicity[currentNote - 1][1] =
                (inharmonicity[currentNote - 1][1] * inharmonicity[currentNote - 1][2] +
                 indCount) / (inharmonicity[currentNote - 1][2] + 1);
    }
    inharmonicity[currentNote - 1][2]++; // Count the number of times B has been measured
    if (inharmonicity[currentNote - 1][2] > 10) {
        inharmonicity[currentNote - 1][2] = 10;
    }

    log_array("BANALYSIS_INHARMONICITY", inharmonicity[currentNote - 1], 3);

    // Store data on the relative height of detected peaks
    for (int i = 0; i < indCount; i++) {
        int idx = (int) xFit[i] - 1;
        if (idx < 0 || idx >= 16) {
            LOGW("Wrong harmonic number %d. Skipping", idx);
            continue;
        }
        double d1 =
                (peakHeights[currentNote - 1][idx]) * (inharmonicity[currentNote - 1][2]) +
                (peaksValues[i]);
        double d2 = (inharmonicity[currentNote - 1][2] + 1);
        peakHeights[currentNote - 1][idx] = d1 / d2;
    }

    // Call method to calculate the best fit and tuning curve
    calculateBxAndTc(false);
}

void ToneDetector::runTcCalculator(bool clearCentsOffsetAvgPlot) {
    for (int n = 0; n < NOTES_ON_PIANO; n++) {
        int nn = n + 1;
        double v1 =
                bxfit[0] * exp(bxfit[1] * nn) + bxfit[2] * exp(bxfit[3] * (nn - NOTES_ON_PIANO));
        if (inharmonicity[n][1] >= 2) {
            double weightMultiplier = inharmonicityWeightMultiplier;
            Bave[n] = (1 * v1 + (inharmonicity[n][0]) *
                                (sqrt(inharmonicity[n][2]) * (sqrt(inharmonicity[n][1] - 2) / 3) *
                                 weightMultiplier)) /
                      (sqrt(inharmonicity[n][2]) * (sqrt(inharmonicity[n][1] - 2) / 3) *
                       weightMultiplier + 1);
            // Weighted average of measured b and fit curve
        } else {
            Bave[n] = v1; //Just take the fit curve if b has not been measured
        }
    }

    /*Make small adjustments to the cent deviations of the harmonics based
     on measurements. The "weight" of the adjust increases with the number
     of measurements (or inharmonicity(:,3))*/

    for (int n = 0; n < 16; n++) {
        int nn = n + 1;
        for (int i = 0; i < NOTES_ON_PIANO; i++) {
            centsAve[i][n] = (600 * log2((1 + (nn * nn) * Bave[i]) / (1 + Bave[i])));
        }
    }

    performPeakHeightsSmoothing();

    // Convert the peak heights to a new scale related to loudness
    double peakHeightsCorrected[NOTES_ON_PIANO][16] = {0.0};
    // call a method that calculate peakHeightsCorrected based on L values
    calculatePeakHeightsCorrected(peakHeightsSmooth, peakHeightsCorrected);

    //% Run tuning curve calculator (calculate "delta")
    // call tcCalculator function to get delta values.
    tcCalculatorP(centsAve, peakHeightsCorrected, delta);

    pianoKeyFrequencies.computeTargetFrequencies(delta, Bave);
    if (clearCentsOffsetAvgPlot) {
        memset(centsOffsetAvgPlot, 0, sizeof(centsOffsetAvgPlot));
    }
    calculateCentsOffsetAvgPlotArray();
}

void ToneDetector::performPeakHeightsSmoothing() {
    // moving average of peak heights
    double peakHeightsSmoothTemp[NOTES_ON_PIANO][16] = {0.0};
    for (int n = 0; n < 10; n++) {
        double curCol[NOTES_ON_PIANO] = {0.0};
        for (int i = 0; i < NOTES_ON_PIANO; i++) {
            curCol[i] = peakHeights[i][n];
        }
        double fs[NOTES_ON_PIANO] = {0.0};
        MathUtils::fastsmooth(curCol, fs, 11, 3, 1, NOTES_ON_PIANO);
        for (int i = 0; i < NOTES_ON_PIANO; i++) {
            peakHeightsSmoothTemp[i][n] = fs[i];
        }
    }

    // calculate best-fits to a model

    // Partial 1
    for (int i = 0; i < NOTES_ON_PIANO; ++i) {
        if (peakHeights[i][0] > 0.999) {
            peakHeights[i][0] = peakHeightsGuess[i][0];
        }
    }
    double peakHeightsSmoothTempP1[NOTES_ON_PIANO] = {0.0};
    {
        double curCol[NOTES_ON_PIANO] = {0.0};
        for (int i = 0; i < NOTES_ON_PIANO; i++) {
            curCol[i] = peakHeights[i][0];
        }
        double fs[NOTES_ON_PIANO] = {0.0};
        MathUtils::fastsmooth(curCol, fs, 3, 3, 1, NOTES_ON_PIANO);
        for (int i = 0; i < NOTES_ON_PIANO; i++) {
            peakHeightsSmoothTempP1[i] = (fs[i] + 99 * peakHeights[i][0]) / 100.0;
        }
    }
    double xFit[NOTES_ON_PIANO + 2] = {0.0};
    double yFit[NOTES_ON_PIANO + 2] = {0.0};
    xFit[0] = 1;
    yFit[0] = log(1.0 / 0.00001 - 1);
    int counter = 1;
    for (int i = 0; i < NOTES_ON_PIANO; ++i) {
        if (peakHeights[i][0] > 0) {
            xFit[counter] = i + 1;
            yFit[counter] = log(1.0 / peakHeightsSmoothTempP1[i] - 1);
            counter++;
        }
    }
    xFit[counter] = NOTES_ON_PIANO;
    yFit[counter] = log(1.0 / 0.99999 - 1);
    counter++;

    double phpv1[2] = {0.0};
    MathUtils::polyFit(xFit, counter, yFit, 1, phpv1);

    double peakHeightsFit[NOTES_ON_PIANO][16] = {0.0};
    for (int i = 0; i < NOTES_ON_PIANO; ++i) {
        peakHeightsFit[i][0] = 1.0 / (1.0 + exp(phpv1[0] * ((i + 1) + phpv1[1] / phpv1[0])));
    }

    // Partial 2
    xFit[0] = 1;
    yFit[0] = log(peakHeightsGuess[0][1]);
    counter = 1;
    for (int i = 0; i < NOTES_ON_PIANO; ++i) {
        if (peakHeights[i][1] > 0.1) {
            xFit[counter] = i + 1;
            yFit[counter] = log(peakHeightsSmoothTemp[i][1]);
            counter++;
        }
    }
    xFit[counter] = NOTES_ON_PIANO;
    yFit[counter] = log(peakHeightsGuess[NOTES_ON_PIANO - 1][1]);
    counter++;

    double phpv2[3] = {0.0};
    MathUtils::polyFit(xFit, counter, yFit, 2, phpv2);
    double out[NOTES_ON_PIANO] = {0.0};
    double noteIndices[NOTES_ON_PIANO] = {0.0};
    for (int noteIndex = 0; noteIndex < NOTES_ON_PIANO; ++noteIndex) {
        noteIndices[noteIndex] = noteIndex + 1;
    }
    MathUtils::polyVal(phpv2, 3, noteIndices, NOTES_ON_PIANO, out);
    for (int i = 0; i < NOTES_ON_PIANO; ++i) {
        peakHeightsFit[i][1] = exp(out[i]);
    }

    // Partials 3-10
    for (int partial = 2; partial < 10; ++partial) {
        counter = 0;
        for (int j = 0; j < NOTES_ON_PIANO; ++j) {
            if (peakHeights[j][partial] > 0.1) {
                xFit[counter] = j + 1;
                yFit[counter] = log(peakHeightsSmoothTemp[j][partial]);
                counter++;
            }
        }
        xFit[counter] = NOTES_ON_PIANO;
        yFit[counter] = log(peakHeightsGuess[NOTES_ON_PIANO - 1][partial]);
        counter++;

        double phpvi[3] = {0.0};
        MathUtils::polyFit(xFit, counter, yFit, 2, phpvi);

        double outi[NOTES_ON_PIANO] = {0.0};
        MathUtils::polyVal(phpvi, 3, noteIndices, NOTES_ON_PIANO, outi);

        for (int j = 0; j < NOTES_ON_PIANO; ++j) {
            peakHeightsFit[j][partial] = exp(outi[j]);
        }
    }

    for (int i = 0; i < NOTES_ON_PIANO; ++i) {
        for (int j = 0; j < 16; ++j) {
            peakHeightsSmooth[i][j] = (peakHeightsSmoothTemp[i][j] + peakHeightsFit[i][j]) / 2.0;
        }
    }
}


double ToneDetector::calculateCentsOffsetAvgPlot(int noteZeroIndexed) {
    double centsOffsetPlot = 0;
    double sumCentsMeasured = 0;
    double sumMeasured = 0;
    for (int partial = 0; partial < MAX_PARTIALS; partial++) {
        if (tuningPartials.get(partial, noteZeroIndexed) != 0) {
            int tuningPartialIndex = tuningPartials.get(partial, noteZeroIndexed) - 1;
            centsOffsetPlot = 1200.0 * log2(harmonics[noteZeroIndexed][tuningPartialIndex] /
                                            pianoKeyFrequencies.targetFrequency(tuningPartialIndex,
                                                                                noteZeroIndexed));
            if (fabs(centsOffsetPlot) <= 150) {
                sumCentsMeasured +=
                        centsOffsetPlot * peakHeights[noteZeroIndexed][tuningPartialIndex];
                sumMeasured += peakHeights[noteZeroIndexed][tuningPartialIndex];
            }
        }
    }
    if (sumMeasured == 0)
        return 0;
    return sumCentsMeasured / sumMeasured;
}

void ToneDetector::calculateCentsOffsetAvgPlotArray() {
    for (int n = 0; n < NOTES_ON_PIANO; n++) {
        double sum = 0.0;
        for (int i = 0; i < 10; i++) {
            sum += harmonics[n][i];
        }
        if (sum > 0.0) {
            centsOffsetAvgPlot[n] = calculateCentsOffsetAvgPlot(n);
        }
    }
}

// This method called in tcCalculator function to generate the assembleSystemMatrix
void ToneDetector::assembleSystemMatrix(double cents[NOTES_ON_PIANO][16],
                                        double peakHeights[NOTES_ON_PIANO][16],
                                        double M[NOTES_ON_PIANO][NOTES_ON_PIANO]) {
    double p_cents_pkf[NOTES_ON_PIANO][16] = {0.0};
    for (int noteZeroIndexed = 0; noteZeroIndexed < NOTES_ON_PIANO; noteZeroIndexed++) {
        for (int j = 0; j < 16; j++) {
            p_cents_pkf[noteZeroIndexed][j] =
                    pianoKeyFrequencies.frequency(noteZeroIndexed) * (j + 1) *
                    powf(2, (cents[noteZeroIndexed][j] / 1200.0));
        }
        memset(M[noteZeroIndexed], 0, sizeof(double) * NOTES_ON_PIANO);
    }

    double octaveWidth = 0;
    double fifthWidth = 0;
    double fourthWidth = 0;
    double twelfthWidth = 0;
    double doubleOctaveWidth = 0;
    double tripleOctaveWidth = 0;
    double major3rdWidth = 0;
    double minor3rdWidth = 0;

    for (int noteZeroIndexed = 12; noteZeroIndexed < NOTES_ON_PIANO; noteZeroIndexed++) {
        AddRowToSystem(noteZeroIndexed, 0, noteZeroIndexed - 12, 1,
                       intervalWeights.octave21() *
                       (pianoKeyFrequencies.frequency(noteZeroIndexed) * 1),
                       octaveWidth, M,
                       peakHeights, p_cents_pkf);
        AddRowToSystem(noteZeroIndexed, 1, noteZeroIndexed - 12, 3,
                       intervalWeights.octave42() *
                       (pianoKeyFrequencies.frequency(noteZeroIndexed) * 2),
                       octaveWidth, M,
                       peakHeights, p_cents_pkf);
        AddRowToSystem(noteZeroIndexed, 2, noteZeroIndexed - 12, 5,
                       intervalWeights.octave63() *
                       (pianoKeyFrequencies.frequency(noteZeroIndexed) * 3),
                       octaveWidth, M,
                       peakHeights, p_cents_pkf);
    }
    for (int noteZeroIndexed = 12; noteZeroIndexed < NOTES_ON_PIANO; noteZeroIndexed++) {
        AddRowToSystem(noteZeroIndexed, 3, noteZeroIndexed - 12, 7,
                       intervalWeights.octave84() *
                       (pianoKeyFrequencies.frequency(noteZeroIndexed) * 4),
                       octaveWidth, M,
                       peakHeights, p_cents_pkf);
    }
    for (int noteZeroIndexed = 12; noteZeroIndexed < NOTES_ON_PIANO; noteZeroIndexed++) {
        AddRowToSystem(noteZeroIndexed, 4, noteZeroIndexed - 12, 9,
                       intervalWeights.octave105() *
                       (pianoKeyFrequencies.frequency(noteZeroIndexed) * 5),
                       octaveWidth, M,
                       peakHeights, p_cents_pkf);
    }
    for (int noteZeroIndexed = 7; noteZeroIndexed < NOTES_ON_PIANO; noteZeroIndexed++) {
        AddRowToSystem(noteZeroIndexed, 1, noteZeroIndexed - 7, 2,
                       intervalWeights.fifth32() *
                       (pianoKeyFrequencies.frequency(noteZeroIndexed) * 2),
                       fifthWidth, M,
                       peakHeights, p_cents_pkf);
        AddRowToSystem(noteZeroIndexed, 3, noteZeroIndexed - 7, 5,
                       intervalWeights.fifth64() *
                       (pianoKeyFrequencies.frequency(noteZeroIndexed) * 4),
                       fifthWidth, M,
                       peakHeights, p_cents_pkf);
    }
    for (int noteZeroIndexed = 5; noteZeroIndexed < NOTES_ON_PIANO; noteZeroIndexed++) {
        AddRowToSystem(noteZeroIndexed, 2, noteZeroIndexed - 5, 3,
                       intervalWeights.fourth43() *
                       (pianoKeyFrequencies.frequency(noteZeroIndexed) * 3),
                       fourthWidth, M,
                       peakHeights, p_cents_pkf);
        AddRowToSystem(noteZeroIndexed, 5, noteZeroIndexed - 5, 7,
                       intervalWeights.fourth86() *
                       (pianoKeyFrequencies.frequency(noteZeroIndexed) * 6),
                       fourthWidth, M,
                       peakHeights, p_cents_pkf);
    }
    for (int noteZeroIndexed = 24; noteZeroIndexed < NOTES_ON_PIANO; noteZeroIndexed++) {
        AddRowToSystem(noteZeroIndexed, 0, noteZeroIndexed - 24, 3,
                       intervalWeights.doubleOctave41() *
                       (pianoKeyFrequencies.frequency(noteZeroIndexed) * 1),
                       doubleOctaveWidth,
                       M, peakHeights, p_cents_pkf);
        AddRowToSystem(noteZeroIndexed, 1, noteZeroIndexed - 24, 7,
                       intervalWeights.doubleOctave82() *
                       (pianoKeyFrequencies.frequency(noteZeroIndexed) * 2),
                       doubleOctaveWidth,
                       M, peakHeights, p_cents_pkf);
    }
    for (int noteZeroIndexed = 36; noteZeroIndexed < NOTES_ON_PIANO; noteZeroIndexed++) {
        AddRowToSystem(noteZeroIndexed, 0, noteZeroIndexed - 36, 7,
                       intervalWeights.tripleOctave() *
                       (pianoKeyFrequencies.frequency(noteZeroIndexed) * 1),
                       tripleOctaveWidth, M,
                       peakHeights, p_cents_pkf);
    }
    for (int noteZeroIndexed = 19; noteZeroIndexed < NOTES_ON_PIANO; noteZeroIndexed++) {
        AddRowToSystem(noteZeroIndexed, 0, noteZeroIndexed - 19, 2,
                       intervalWeights.twelfth31() *
                       (pianoKeyFrequencies.frequency(noteZeroIndexed) * 1),
                       twelfthWidth, M,
                       peakHeights, p_cents_pkf);
        AddRowToSystem(noteZeroIndexed, 1, noteZeroIndexed - 19, 5,
                       intervalWeights.twelfth62() *
                       (pianoKeyFrequencies.frequency(noteZeroIndexed) * 2),
                       twelfthWidth, M,
                       peakHeights, p_cents_pkf);
        AddRowToSystem(noteZeroIndexed, 2, noteZeroIndexed - 19, 8,
                       intervalWeights.twelfth93() *
                       (pianoKeyFrequencies.frequency(noteZeroIndexed) * 3),
                       twelfthWidth, M,
                       peakHeights, p_cents_pkf);
    }
    for (int noteZeroIndexed = 31; noteZeroIndexed < NOTES_ON_PIANO; ++noteZeroIndexed) {
        AddRowToSystem(noteZeroIndexed, 0, noteZeroIndexed - 31, 5,
                       intervalWeights.nineteenth61() *
                       (pianoKeyFrequencies.frequency(noteZeroIndexed) * 1),
                       0,
                       M,
                       peakHeights, p_cents_pkf);
    }
    // major 3rd
    for (int noteZeroIndexed = 4; noteZeroIndexed < 49; noteZeroIndexed++) {
        AddRowToSystem(noteZeroIndexed, 3, noteZeroIndexed - 4, 4,
                       intervalWeights.third54() *
                       (pianoKeyFrequencies.frequency(noteZeroIndexed) * 4),
                       major3rdWidth, M,
                       peakHeights, p_cents_pkf);
    }
    // minor 3rd
    for (int noteZeroIndexed = 3; noteZeroIndexed < 49; noteZeroIndexed++) {
        AddRowToSystem(noteZeroIndexed, 4, noteZeroIndexed - 3, 5,
                       intervalWeights.third65() *
                       (pianoKeyFrequencies.frequency(noteZeroIndexed) * 5),
                       minor3rdWidth, M,
                       peakHeights, p_cents_pkf);
    }
}

// this method called during generating assembleSystemMatrix in tcCalculator function
void ToneDetector::AddRowToSystem(int n1, int p1, int n2, int p2, double w, double width,
                                  double M[NOTES_ON_PIANO][NOTES_ON_PIANO],
                                  double peakHeights[NOTES_ON_PIANO][16],
                                  double p_cents_pkf[NOTES_ON_PIANO][16]) {
    double wlocal = peakHeights[n1][p1] * peakHeights[n2][p2] / w;
    double A1 = p_cents_pkf[n1][p1] * pow(2, -width / 2 / 1200);
    double A2 = -p_cents_pkf[n2][p2] * pow(2, -width / 2 / 1200);

    M[n1][n1] = M[n1][n1] + A1 * A1 * wlocal;
    M[n2][n2] = M[n2][n2] + A2 * A2 * wlocal;
    M[n1][n2] = M[n1][n2] + A1 * A2 * wlocal;
    M[n2][n1] = M[n1][n2];

    if (n1 == 12 && n2 == 0) {
//        LOGD("A1: %f A2: %f wlocal: %f w: %f", A1, A2, wlocal, w);
    }
}

// this method is called in bAnalysis function to calculate tuning curve
void ToneDetector::tcCalculatorP(double centsAvg[NOTES_ON_PIANO][16],
                                 double peakHeightsCorrected[NOTES_ON_PIANO][16],
                                 double *delta) {
    double M[NOTES_ON_PIANO][NOTES_ON_PIANO] = {0.0};
    assembleSystemMatrix(centsAvg, peakHeightsCorrected, M);

    double xFull[NOTES_ON_PIANO] = {0.0};
    double xReduced[NOTES_ON_PIANO - 1] = {0.0};
    double RHS[NOTES_ON_PIANO - 1] = {0.0};
    double M2[NOTES_ON_PIANO - 1][NOTES_ON_PIANO - 1] = {0.0};

    xFull[48] = 1;

    for (int i = 0; i < NOTES_ON_PIANO; i++) {
        if (i == 48)continue;
        if (i < 48) {
            for (int j = 0; j < NOTES_ON_PIANO; j++) {
                if (j == 48)continue;
                if (j < 48) {
                    M2[i][j] = M[i][j];
                } else if (j > 48) {
                    M2[i][j - 1] = M[i][j];
                }
            }
            RHS[i] = -M[i][48];
        } else if (i > 48) {
            for (int j = 0; j < NOTES_ON_PIANO; j++) {
                if (j == 48)continue;
                if (j < 48) {
                    M2[(i - 1)][j] = M[i][j];
                } else if (j > 48) {
                    M2[(i - 1)][j - 1] = M[i][j];
                }
            }
            RHS[i - 1] = -M[i][48];
        }
    }

    double M3[NOTES_ON_PIANO - 1][NOTES_ON_PIANO - 1] = {0.0};

    MathUtils::determineCholeskyMatrix((NOTES_ON_PIANO - 1), M2, M3);
    MathUtils::solveCholeskyMatrix((NOTES_ON_PIANO - 1), M3, RHS, xReduced);

    for (int i = 0; i < NOTES_ON_PIANO; i++) {
        if (i == 48) {
            delta[i] = 0;
            continue;
        } else if (i < 48) {
            xFull[i] = xReduced[i];
        } else {
            xFull[i] = xReduced[i - 1];
        }
        if (xFull[i] != 0) {
            delta[i] = log2(xFull[i]) * 1200;
            if (delta[i] < -100) {
                delta[i] = -100;
            }
        } else {
            delta[i] = 0;
        }
    }

    for (int i = 0; i < NOTES_ON_PIANO; i++) {
        delta[i] += temperament[i % TEMPERAMENT_SIZE];
    }

    intervalWeights.applyStretchOffsets(delta);
    for (int i = 0; i < NOTES_ON_PIANO; ++i) {
        if (delta[i] < -100) {
            delta[i] = -100;
        }
    }

    pianoKeyFrequencies.computeTargetFrequencies(delta, Bave);
}

void ToneDetector::calculatePeakHeightsCorrected(double peakHeightsSmooth[NOTES_ON_PIANO][16],
                                                 double out[NOTES_ON_PIANO][16]) {

    for (int n = 0; n < 16; n++) {
        int nn = n + 1;
        for (int noteZeroIndexed = 0; noteZeroIndexed < NOTES_ON_PIANO; noteZeroIndexed++) {
            double pf = pianoKeyFrequencies.frequency(noteZeroIndexed) * nn;
            double Rb = ((12200 * 12200) * powf(pf, 3)) /
                        (((pf * pf) + (20.6 * 20.6)) * ((pf * pf) + (12200 * 12200)) *
                         sqrtf(((pf * pf) + (158.5 * 158.5))));
            if (n < 10) {
                out[noteZeroIndexed][n] = Rb * peakHeightsSmooth[noteZeroIndexed][n];
            } else {
                out[noteZeroIndexed][n] = 0;
            }
        }
    }
}

/** this method return the index of max value in the list,
 * it is called in detectNote function to get the index of max peak*/
int ToneDetector::getMaxIndex(double *values, int n) {
    if (n == 1) {
        return 0;
    }
    int maxIndex = 0;
    double max = values[0];

    for (int i = 1; i < n; i++) {
        if (values[i] > max) {
            max = values[i];
            maxIndex = i;
        }
    }
    return maxIndex;
}

/** this method implements diff function in matlab
 * it is called in calculateFreqRange function to get the value of df variable
 * also it is called in bAnalysis function */
void ToneDetector::calculateDiff(double *arr, double *diff, int len) {
    for (int i = 0; i < len - 1; i++) {
        diff[i] = arr[i + 1] - arr[i];
    }
}

/** implementation of closest key function
 * it is called in detectNote function
 **/
int ToneDetector::closestKey(double freq) {
    return adjustKeyForPitchRaise(pianoKeyFrequencies.closestKey(freq));
}

int ToneDetector::closestKeyP12(double freq) {
    return adjustKeyForPitchRaise(pianoKeyFrequencies.closestKeyP12(freq));
}

int ToneDetector::adjustKeyForPitchRaise(int key) {
    int index = key;
    // A key can be -150 cents off during pitch raise
    // Select the next higher key if the closest key is not selected
    // and the next higher key is selected
    if (isPitchRaiseMeasurementOnFlag &&
        pitchRaiseKeys[((index - 1) % 12)] == 0 &&
        pitchRaiseKeys[((index) % 12)] &&
        index < NOTES_ON_PIANO) {
        index++;
    }
    return index;
}

bool ToneDetector::isSilence(short *buffer, unsigned int bufferLen) {
    double sum = 0;
    for (unsigned int i = 0; i < bufferLen; i++) {
        sum += (double) abs(buffer[i]);
    }

    return (INV_SHORT_MAX * sum / (double) bufferLen) < 0.1F * minAmplitude;
}

void ToneDetector::clearZeroCrossing() {
    for (int partial = 0; partial < targetPeakFreqLen; partial++) {
        filteredData[partial].fill(0.0F);
        centsOffsetZC[partial].fill(0.0F);
        centsOffsetZCFiltered[partial] = 0.0F;
        dataSumOld = 0.0F;
        clearPhase(partial);
    }
    centsOffsetZCAve.clear();
    centsOffsetCombined = 0.0F;
    qualityTestCombined = false;
}

void ToneDetector::setZeroCrossingFreq() {
    for (int partial = 0; partial < targetPeakFreqLen; partial++) {
        double freq = targetPeakFreq[partial];
        double overpull = pow(2.0, overpullCents[currentNote - 1] / 1200.0);
        cyclesPerSample[partial] = overpull * freq / sampleRate;
        bpf[partial][0].setParam(freq, BANDPASS_FILTER_BANDWIDTH);
        bpf[partial][1].setParam(freq, BANDPASS_FILTER_BANDWIDTH);
    }
    wideBpf.setParam(targetFreqGeometricMean[currentNote - 1],
                     targetFreqBandwidth[currentNote - 1]);
}

bool ToneDetector::fastLoop() {

    int bufferLen = INTERNAL_FRAME_LEN;
    float fbuffer[INTERNAL_FRAME_LEN] = {0.0F};
    double peakHeightSum = 0;
    double denominator = 0;
    double stdCentsOffset = 0;

    {
        std::lock_guard<std::mutex> lock(bufferMutex);
        // do we have data?
        if (ringBufferWriteCounter - ringBufferFastLoopReadCounter < bufferLen / 2) {
            return false;
        }
        // shift the overlap buffer
        /* Removed 5/2019 because it was splicing non-contiguous audio data on some devices
        memmove(overlapBuffer, overlapBuffer + bufferLen / 2, sizeof(short) * bufferLen / 2);
        // get the latest bufferLen/2 samples
        getBuffer(overlapBuffer + bufferLen / 2, bufferLen / 2,
                  ringBufferWriteCounter - bufferLen / 2);
        ringBufferFastLoopReadCounter = ringBufferWriteCounter;
         */

        //Try copying the entire array out of the buffer instead of shifting 5/2019
        getBuffer(overlapBuffer, bufferLen, ringBufferWriteCounter - bufferLen);
        ringBufferFastLoopReadCounter = ringBufferWriteCounter;
    }

    // Compare the left side of the data window to the right side.
    // If the right side is more than 3x heavier, we abort the fast loop.

    long sumLeftFL = 0, sumRightFL = 0;
    for (int i = 0; i < INTERNAL_FRAME_LEN / 2; i++) {
        sumLeftFL += abs(overlapBuffer[i]);
        sumRightFL += abs(overlapBuffer[i + INTERNAL_FRAME_LEN / 2]);
    }
    sumTotalFL = sumLeftFL + sumRightFL;
    fastLoopMode = 10;
    if (sumLeftFL * 3 > sumRightFL) {
        fastLoopMode = 20;

        // apply windowing
        for (int i = 0; i < INTERNAL_FRAME_LEN; i++) {
            fbuffer[i] = overlapBuffer[i] * window[i];
        }

        // take fft and power spectrum
        kiss_fft_cpx buf[INTERNAL_FRAME_LEN / 2 + 1];
        // 4k FFT
        kiss_fftr(cfgFastLoop, fbuffer, buf);

        // take abs (yDFT)
        double maxAmplitude = -10000000;
        int i;
        for (i = 0; i < INTERNAL_FRAME_LEN / 2; i++) {
            fbuffer[i] = cpx_abs(buf[i]);

            // record the harmonic with the maximum amplitude
            if (fbuffer[i] > maxAmplitude) {
                maxAmplitude = fbuffer[i];
            }
        }
        for (i = INTERNAL_FRAME_LEN / 2; i < INTERNAL_FRAME_LEN; i++) {
            fbuffer[i] = 0.0F;
        }

        //Detect peaks around the target frequencies (note we are often measuring
        // more than one frequency per note). targetFreq is an array containing
        // calculated frequencies for the first 16 partials of each note.

        double centsOffset[MAX_PEAKS] = {0.0};

        int centsOffsetValues = centsOffsetCalc(fbuffer, maxAmplitude, centsOffset);

        // Automatic stepwise note switching if measured frequencies are significantly
        //far away from target and tightly grouped (not noise)
        double meanCentsOffset = MathUtils::mean(centsOffset, 0, centsOffsetValues);
        stdCentsOffset = MathUtils::std(centsOffset, centsOffsetValues);
        if (noteChangeMode != NOTE_CHANGE_LOCK && !isPitchRaiseMeasurementOnFlag) {

            // Need to calculate quality test before switching notes (it is calculated again later below)
            peakHeightSum = 0;
            for (int i = 0; i < measuredPeakHeightLen; i++) {
                if (goodPeaksIndex[i] == 1) {
                    peakHeightSum += measuredPeakHeight[i];
                }
            }
            qualityTest = stdCentsOffset < 10 && peakHeightSum > 0.45;

            if (meanCentsOffset > 60 && stdCentsOffset < 10 && currentNote < NOTES_ON_PIANO &&
                qualityTest) {
                previousNote = currentNote;
                setCurrentNote(currentNote + 1, false, false);
                centsOffsetValues = centsOffsetCalc(fbuffer, maxAmplitude, centsOffset);
                //meanCentsOffset = MathUtils::mean(centsOffset,0,centsOffsetValues);
                stdCentsOffset = MathUtils::std(centsOffset, centsOffsetValues);
            } else if (meanCentsOffset < -60 && stdCentsOffset < 10 && currentNote > 1 &&
                       qualityTest) {
                previousNote = currentNote;
                setCurrentNote(currentNote - 1, false, false);
                centsOffsetValues = centsOffsetCalc(fbuffer, maxAmplitude, centsOffset);
                //meanCentsOffset = MathUtils::mean(centsOffset,0,centsOffsetValues);
                stdCentsOffset = MathUtils::std(centsOffset, centsOffsetValues);
            }
            if (currentNote != previousNote && previousNote > 0) {  // if note was changed above
                centsOffsetAvgPlot[previousNote - 1] = calculateCentsOffsetAvgPlot(
                        previousNote - 1);
            }

            qualityTest = 0; // reset to zero so it can be recalculated later
        }

        // Take a weighted average of the offsets of various peaks. This will drive
        // the dial indicator. Since we are measuring a different number of peaks
        // for each note we have to combine them giving them weight according to the
        // peak heights.

        centsOffsetAvg = 0;
        denominator = 0;

        for (int i = 0; i < measuredPeakHeightLen; i++) {
            if (goodPeaksIndex[i] == 1) {
                centsOffsetAvg += centsOffsetFull[i] * measuredPeakHeight[i];
                denominator += measuredPeakHeight[i];
            }
        }

//        LOGV("CENTSOFFSET: Mfh:[%f,%f,%f,%f,%f] c0:[%f,%f,%f,%f,%f] gP:[%d,%d,%d,%d,%d] %d %f %f\n",
//             measuredPeakHeight[0], measuredPeakHeight[1], measuredPeakHeight[2],
//             measuredPeakHeight[3], measuredPeakHeight[4], centsOffsetFull[0],
//             centsOffsetFull[1], centsOffsetFull[2], centsOffsetFull[3],
//             centsOffsetFull[4], goodPeaksIndex[0], goodPeaksIndex[1],
//             goodPeaksIndex[2], goodPeaksIndex[3], goodPeaksIndex[4],
//             measuredPeakHeightLen, centsOffsetAvg, denominator);

        if (denominator > 0 && denominator < 1000)
            centsOffsetAvg = centsOffsetAvg / denominator;
        else
            centsOffsetAvg = 0;

        // Test of a new method of smoothing centsOffsetAvg (low pass filter)
        //double stdCentsOffset = MathUtils::std(centsOffset, centsOffsetValues);

        qualityTest = stdCentsOffset < 10 && denominator > 0.45;
        fastLoopMode = 40;

    } else {
        qualityTest = 0;
        fastLoopMode = 30;
    }

    fastLoopMode = fastLoopMode * 10;

    if (qualityTest) {
        fastLoopMode += 2;

        if (currentNote != previousNoteF) {
            centsOffsetAvgXMem1 = centsOffsetAvg; // single pole median filter
            centsOffsetAvgXMem2 = 0.0;
            previousNoteF = currentNote;
            centsOffsetAvgLPF = centsOffsetAvg;
            LOGV("FASTLOOP: coALPF:%f coA:%f\n", centsOffsetAvgLPF, centsOffsetAvg);
        } else {
            // Median, single pole, and  linear regression filter for centsOffsetAvg
            double centsOffset[3] = {centsOffsetAvg, centsOffsetAvgXMem1, centsOffsetAvgXMem2};
            double centsOffsetMedian = MathUtils::median(centsOffset, 3);
            if (centsOffsetMedian == centsOffsetAvg) {
                centsOffsetAvgLPF = 0.5 * (centsOffsetAvgLPF + centsOffsetMedian);
            } else {
                double x[3] = {0.0, 1.0, 2.0};
                double filterFitCoef[2];
                // Simple linear regression of the last 3 data points in reverse order
                MathUtils::polyFit(x, 3, centsOffset, 1, filterFitCoef);
                // Use the y intercept to estimate the latest data point, then LPF
                centsOffsetAvgLPF = 0.5 * (centsOffsetAvgLPF + filterFitCoef[1]);
            }
            centsOffsetAvgXMem2 = centsOffsetAvgXMem1;
            centsOffsetAvgXMem1 = centsOffsetAvg;
        }
    }

    if (!qualityTest) {
        fastLoopMode += 1;
        sumTotalFLOld = sumTotalFL;
//        LOGV("FASTLOOP: denominator %f stdCentsOffset %f \n", denominator, stdCentsOffset);
        return true;
    }

    angle = 90.0F * scaleFcn(centsOffsetAvgLPF);
    offsetOver = fabs(centsOffsetAvgLPF) > 100; // hides needle if offset is out of range
    num = roundf(centsOffsetAvgLPF * 10.0F) / 10.0F;
    fastLoopMode += 4;

    int goodPeakCount = 0; // Count the number of peaks that have survived the quality testing
    for (int gpc = 0; gpc < measuredPeakHeightLen; gpc++) {
        if (goodPeaksIndex[gpc] == 1) {
            goodPeakCount++;
        }
    }

    // Store data for Harmonics and peakHeights
    for (int ii = 0; ii < measuredPeakHeightLen; ii++) {
        if (measuredPeakFreq[ii] != 0) {
            if (fabs(measuredPeakFreq[ii] - measuredPeakFreqOld[ii]) / measuredPeakFreq[ii] <
                0.006) // less than 10 cent change from previous measurement
            {
                if (qualityTest && sumTotalFL < sumTotalFLOld) {
                    //If pitch raise measurement is active require a minimum number of detected peaks
                    if (!isPitchRaiseMeasurementOnFlag || goodPeakCount > 2 ||
                        (goodPeakCount > 1 && currentNote > 40) || currentNote > 55) {
                        if (harmonics[currentNote - 1][targetPartials[ii]] == 0) {
                            harmonics[currentNote - 1][targetPartials[ii]] = measuredPeakFreq[ii];
                        } else {
                            harmonics[currentNote - 1][targetPartials[ii]] =
                                    0.7 * harmonics[currentNote - 1][targetPartials[ii]] +
                                    0.3 *
                                    measuredPeakFreq[ii]; // Weighted average (low pass filter)
                        }
                        peakHeights[currentNote - 1][targetPartials[ii]] =
                                0.7 * peakHeights[currentNote - 1][targetPartials[ii]] +
                                0.3 * measuredPeakHeight[ii];
                    }
                }
            }
            measuredPeakFreqOld[ii] = measuredPeakFreq[ii];
        }
    }
    sumTotalFLOld = sumTotalFL;

    return true;
}

void ToneDetector::processZeroCrossing() {
    while (ringBufferWriteCounter - ringBufferZeroCrossingReadCounter >= ZERO_CROSSING_FRAME_LEN) {
        short zcBuffer[ZERO_CROSSING_FRAME_LEN] = {0};
        getBuffer(zcBuffer, ZERO_CROSSING_FRAME_LEN, ringBufferZeroCrossingReadCounter);
        processZeroCrossingBuffer(zcBuffer, ZERO_CROSSING_FRAME_LEN);
        ringBufferZeroCrossingReadCounter += ZERO_CROSSING_FRAME_LEN;
    }
}

// length % ZERO_CROSSING_FRAME_LEN should be 0
void ToneDetector::processZeroCrossingBuffer(const short *buffer, const int length) {
    if (currentNoteZC != currentNote) {
        clearZeroCrossing();
        currentNoteZC = currentNote;
    }
    if (currentFreqZC != targetPeakFreq[0]) {
        setZeroCrossingFreq();
        currentFreqZC = targetPeakFreq[0];
    }

    float amplitudeThreshold;
    if (targetPeakFreqLen < 2) {
        amplitudeThreshold = 0.3F;
    } else if (targetPeakFreqLen > 3) {
        amplitudeThreshold = 0.15F;
    } else {
        amplitudeThreshold = 0.25F;
    }

    for (int i = 0; i < length; i += ZERO_CROSSING_FRAME_LEN) {
        float dataSum = 0.0F;
        float dataSumBPF[MAX_PARTIALS] = {0.0F};
        float zeroCrossingRefPhase[MAX_PARTIALS] = {0.0F};
        int zeroCrossingIntX[MAX_PARTIALS] = {-1};
        float zeroCrossingY[MAX_PARTIALS][2] = {0.0F};
        int numZeroCrossings[MAX_PARTIALS] = {0};

        // process the frame
        for (int j = 0; j < ZERO_CROSSING_FRAME_LEN; j++) {
            dataSum += fabsf((float) wideBpf.filter((double) buffer[i + j]));
            for (int partial = 0; partial < targetPeakFreqLen; partial++) {
                // apply bandpass filter twice
                float y = (float) bpf[partial][0].filter((double) buffer[i + j]);
                y = (float) bpf[partial][1].filter(y);
                arrayShift(filteredData[partial], y);
                // average amplitude by integrating abs(data)
                dataSumBPF[partial] += fabsf(y);
                // check for rising zero crossing
                if (filteredData[partial][0] < 0.0F && filteredData[partial][1] > 0.0F) {
                    zeroCrossingRefPhase[partial] = (float) referencePhase[partial];
                    zeroCrossingIntX[partial] = j;
                    zeroCrossingY[partial][0] = filteredData[partial][0];
                    zeroCrossingY[partial][1] = filteredData[partial][1];
                    numZeroCrossings[partial]++;
                }
                // increment phase
                referencePhase[partial] += cyclesPerSample[partial];
                // wrap to [0..1]
                if (referencePhase[partial] >= 1.0) {
                    referencePhase[partial] -= 1.0;
                }
            }
        }

        // process the zero crossing
        float phase[MAX_PARTIALS] = {0.0F};
        for (int partial = 0; partial < targetPeakFreqLen; partial++) {
            // interpolate zero crossing sample position
            float zeroCrossingFracX = zeroCrossingY[partial][0] /
                                      (zeroCrossingY[partial][0] - zeroCrossingY[partial][1]);
            float zeroCrossingX = zeroCrossingIntX[partial] + zeroCrossingFracX;
            // phase difference between zero crossing and reference
            phase[partial] = -(zeroCrossingRefPhase[partial] +
                               zeroCrossingFracX * (float) cyclesPerSample[partial]);
            // wrap to [0..1]
            float mod = fmod(phase[partial], 1.0F);
            if (mod < 0) {
                mod += 1.0F;
            }
            phase[partial] = mod;
            if (lastZeroCrossingX[partial] >= 0.0F) {
                // Caclulate total samples since last zero crossing
                float zcTotalSamples =
                        ZERO_CROSSING_FRAME_LEN + zeroCrossingX - lastZeroCrossingX[partial];
                // Calculate the frequency of each harmonic based on counting zero crossings
                float freq = numZeroCrossings[partial] / zcTotalSamples * sampleRate;
                // Convert frequency to cents offset
                arrayShift(centsOffsetZC[partial],
                           1200.0F * MathUtils::log2(freq / targetPeakFreq[partial]));
            }
            lastZeroCrossingX[partial] = zeroCrossingX;
        }

        if (avgAmp == 0.0F) {
            avgAmp = dataSum;
        } else {
            avgAmp = 0.999F * avgAmp + 0.002F * dataSum;
        }

        float dataSumRatio[MAX_PARTIALS] = {0.0F};
        // cents offset such that dataSumRatio > amplitudeThreshold;
        std::vector<float> centsOffsetZCGtThreshold;
        for (int partial = 0; partial < targetPeakFreqLen; partial++) {
            dataSumRatio[partial] = dataSumBPF[partial] / dataSum;
            float alpha = computeSpinnerAlpha(dataSumRatio[partial], amplitudeThreshold);
            if (alpha > 0.0) {
                if (dataSumRatio[partial] > amplitudeThreshold) {
                    centsOffsetZCGtThreshold.push_back(centsOffsetZC[partial].back());
                }
                if (dataSumRatio[partial] < 1.4F) {
                    addPhase(partial, phase[partial], alpha);
                } else {
                    addPhase(partial, SPINNER_INACTIVE, 0.0F);
                }
            } else {
                addPhase(partial, SPINNER_INACTIVE, 0.0F);
            }
        }

        // This counts how many partials/harmonics are above the amplitude threshold and makes sure there are enough
        if (centsOffsetZCGtThreshold.size() >= std::max(targetPeakFreqLen / 2, 1)) {
            for (int partial = 0; partial < targetPeakFreqLen; partial++) {
                // Eliminate outliers
                if (fabs(centsOffsetZC[partial].back() -
                         MathUtils::median(centsOffsetZCGtThreshold)) < 5.0F &&
                    centsOffsetZC[partial].back() != 0.0F &&
                    // Makes sure partial amplitude is above the threshold
                    dataSumRatio[partial] > amplitudeThreshold &&
                    dataSumRatio[partial] < 1.4F &&
                    // If it is too far above the threshold that is a problem (indicates ringing from the filter after a cutoff of the actual signal)
                    dataSum > 0.1F * avgAmp &&
                    dataSum < dataSumOld * 10.0F) {
                    // excludes the first data point when a note is struck (rapidly rising amplitude)
                    centsOffsetZCFiltered[partial] =
                            (float) (0.4 * centsOffsetZCFiltered[partial] +
                                     0.6 * MathUtils::median(centsOffsetZC[partial]));
                } else {
                    centsOffsetZCFiltered[partial] = 0.0F;
                }
            }
        } else {
            memset(centsOffsetZCFiltered, 0, sizeof(centsOffsetZCFiltered));
        }
        dataSumOld = dataSum;

        // Calculate centsOffset using the dataSumRatio to weight individual harmonics
        float centsOffsetZCAveNum = 0.0F;
        float centsOffsetZCAveDen = 0.0F;
        for (int partial = 0; partial < targetPeakFreqLen; partial++) {
            if (centsOffsetZCFiltered[partial] != 0.0F) {
                centsOffsetZCAveNum += centsOffsetZCFiltered[partial] * dataSumRatio[partial];
                centsOffsetZCAveDen += dataSumRatio[partial];
            }
        }
        if (centsOffsetZCAveDen != 0.0F) {
            while (centsOffsetZCAve.size() >= 4) {
                centsOffsetZCAve.erase(centsOffsetZCAve.begin());
            }
            centsOffsetZCAve.push_back(centsOffsetZCAveNum / centsOffsetZCAveDen);
        }
    }
}

void ToneDetector::clearPhase(int partial) {
    std::lock_guard<std::mutex> lock(phaseDataMutex);
    phaseData[partial].clear();
    alphaData[partial].clear();
}

void ToneDetector::addPhase(int partial, float phase, float alpha) {
    std::lock_guard<std::mutex> lock(phaseDataMutex);
    while (phaseData[partial].size() >= PHASE_QUEUE_SIZE) {
        phaseData[partial].erase(phaseData[partial].begin());
    }
    while (alphaData[partial].size() >= PHASE_QUEUE_SIZE) {
        alphaData[partial].erase(alphaData[partial].begin());
    }
    phaseData[partial].push_back(phase);
    alphaData[partial].push_back(alpha);
}

void ToneDetector::getPhase(int partial, std::vector<float> *state) {
    std::lock_guard<std::mutex> lock(phaseDataMutex);
    if (!phaseData[partial].empty()) {
        float phase = phaseData[partial].front();
        phaseData[partial].erase(phaseData[partial].begin());
        state->insert(state->end(), phase);
    } else {
        float phase = SPINNER_EMPTY;
        state->insert(state->end(), phase);
    }

    if (!alphaData[partial].empty()) {
        float alpha = alphaData[partial].front();
        alphaData[partial].erase(alphaData[partial].begin());
        state->insert(state->end(), alpha);
    } else {
        state->insert(state->end(), 0.0F);
    }
}

void ToneDetector::skipPhases(int partial, int numberOfSkippedFrames) {
    std::lock_guard<std::mutex> lock(phaseDataMutex);
    for (int i = 0; i < numberOfSkippedFrames; ++i) {
        // we don't want to clear phase data
        // we want to keep at least one (the newest) point
        bool processed = false;
        if (phaseData[partial].size() > 1) {
            phaseData[partial].erase(phaseData[partial].begin());
            processed = true;
        }
        if (alphaData[partial].size() > 1) {
            alphaData[partial].erase(alphaData[partial].begin());
            processed = true;
        }
        if (!processed) {
            break;
        }
    }
}

float ToneDetector::getCentsOffsetZCAvg() {
    return !centsOffsetZCAve.empty() ? centsOffsetZCAve.back() : 0.0F;
}

float ToneDetector::getCentsOffsetCombined() {
    qualityTestCombined = true;
    if (qualityTest) {
        if (!centsOffsetZCAve.empty()) {
            centsOffsetCombined = 0.5F * (centsOffsetAvgLPF + MathUtils::median(centsOffsetZCAve));
        } else {
            centsOffsetCombined = 0.5F * (centsOffsetCombined + centsOffsetAvgLPF);
        }
    } else {
        if (!centsOffsetZCAve.empty()) {
            float weight = 0.5F * centsOffsetZCAve.size() / 4;
            if (weight > 0.5F) {
                weight = 0.5F;
            }
            centsOffsetCombined = (1.0F - weight) * centsOffsetCombined +
                                  weight * MathUtils::median(centsOffsetZCAve);
        } else {
            qualityTestCombined = false;
        }
    }
    return centsOffsetCombined;
}


double ToneDetector::scaleFcn(double offset) {
    int sign = 0;
    if (offset < 0)sign = -1;
    else if (offset > 0)sign = 1;
    double val1 = powf(fabs(offset), 0.7);
    double val2 = powf(100, 0.7);
    double out = sinf(sign * val1 * PI / 2.0 / val2);
    return out;
}

double ToneDetector::getFreqRangeToneDetector(int freq) {
    int max = sizeof(freqRangeToneDetectorMap) / sizeof(double) - 1;
    if (freq < 0) { freq = 0; }
    if (freq > max) { freq = max; }
    return freqRangeToneDetectorMap[freq];
}

// This calculates centsOffset from snippets of the raw FFT data
int ToneDetector::centsOffsetCalc(float *yDFTToneDetector, double maxAmplitudeToneDetector,
                                  double *centOffset) {
    targetPeakFreqLen = 0;

    // Count which partials are being measured
    for (int partial = 0; partial < MAX_PARTIALS; partial++) {
        if (tuningPartials.get(partial, currentNote - 1) != 0) {
            targetPartials[targetPeakFreqLen] = tuningPartials.get(partial, currentNote - 1) - 1;
            targetPeakFreq[targetPeakFreqLen] = pianoKeyFrequencies.targetFrequency(
                    targetPartials[targetPeakFreqLen], currentNote - 1);
            targetPeakFreqLen++;
        }
    }
    measuredPeakHeightLen = targetPeakFreqLen;

    // Only run peakfinder for frequencies around the target partial frequencies
    int count = 0;
    for (int n = 0; n < targetPeakFreqLen; n++) {
        int start = (int) getFreqRangeToneDetector(
                (int) roundf(targetPeakFreq[n] * MINUS_150_CENTS) - 1) - 3;
        int end = (int) getFreqRangeToneDetector(
                (int) roundf(targetPeakFreq[n] * PLUS_100_CENTS) - 1) + 3;

        int indexSize = end - start + 1;
        int ToneDetectorPFIndex[indexSize];
        for (int i = 0; i < indexSize; i++) {
            ToneDetectorPFIndex[i] = start + i;
        }
        double tempFreq[2048] = {0.0};
        double subYdftToneDetector[2048] = {0.0};

        for (int i = 0; i < indexSize; i++) {
            tempFreq[i] = freqRange[ToneDetectorPFIndex[i]];
            subYdftToneDetector[i] =
                    yDFTToneDetector[ToneDetectorPFIndex[i]] / maxAmplitudeToneDetector;
        }

        double values[MAX_PEAKS] = {0.0};
        double freqs[MAX_PEAKS] = {0.0};
        int indexes[MAX_PEAKS] = {0};
        unsigned int peaks = peakFinder.findPeaks(values, freqs, indexes, tempFreq,
                                                  subYdftToneDetector, 0.01,
                                                  0.1, false, indexSize);
        if (peaks == 0) {
            measuredPeakFreq[n] = 0;
            measuredPeakHeight[n] = 0;
            count++;
        } else {
            // Choose the largest amplitude peak that is closest to the target frequency
            double min = fabs(freqs[0] - targetPeakFreq[n])
                         / values[0];
            int minIndex = 0;
            for (int i = 0; i < peaks; i++) {
                double cur = fabs(freqs[i] - targetPeakFreq[n])
                             / values[i];
                if (cur < min) {
                    min = cur;
                    minIndex = i;
                }
            }
            measuredPeakFreq[n] = freqs[minIndex];
            measuredPeakHeight[n] = values[minIndex];
        }
    }

    isTargetLengthCount = count == targetPeakFreqLen;
    if (isTargetLengthCount)
        return 0;

    int centOffsetValuesCount = 0;
    int measuredPeakHeightIndexes[targetPeakFreqLen];
    double centOffsetSubFull[targetPeakFreqLen];
    int centOffsetSubIndices[targetPeakFreqLen];

    // Throw out values where abs(centsOffset)>150
    for (int i = 0; i < targetPeakFreqLen; i++) {
        double val = 1200 * log2(measuredPeakFreq[i] / targetPeakFreq[i]);
        centsOffsetFull[i] = val;

        if (fabs(val) >= 150) { // corresponds to badPeaksIndex150
            measuredPeakHeightIndexes[i] = 0;
            centsOffsetFull[i] = 0;
            goodPeaksIndex[i] = 0;
            measuredPeakFreq[i] = 0;
            measuredPeakHeight[i] = 0;
        } else {
            goodPeaksIndex[i] = 1;
            centOffsetSubIndices[centOffsetValuesCount] = i;
            centOffset[centOffsetValuesCount] = val;
            centOffsetSubFull[centOffsetValuesCount++] = val;
        }


    }
    measuredPeakHeightLen = targetPeakFreqLen;

    // Take median and standard deviation of the offsets to get rid of outliers
    double medianCentsOffset = MathUtils::median(centOffsetSubFull, centOffsetValuesCount);
    double stdCentsOffset = MathUtils::std(centOffsetSubFull, centOffsetValuesCount);
    if (stdCentsOffset > 10) {
        centOffsetValuesCount = 0;

        for (int i = 0; i < targetPeakFreqLen; i++) {
            // corresponds to badPeaksIndexSTD
            if (fabs(centsOffsetFull[i] - medianCentsOffset) > 10) {
                measuredPeakFreq[i] = 0;
                measuredPeakHeight[i] = 0;
                goodPeaksIndex[i] = 0;
                centsOffsetFull[i] = 0;
            } else {
                if (centsOffsetFull[i] != 0) {
                    centOffset[centOffsetValuesCount++] = centsOffsetFull[i];
                } else {
                    goodPeaksIndex[i] = 0;
                    measuredPeakFreq[i] = 0;
                    measuredPeakHeight[i] = 0;
                    centsOffsetFull[i] = 0;
                }
            }
        }
    }

    return centOffsetValuesCount;
}

void ToneDetector::setOverpullCents(const double *cents) {
    for (int i = 0; i < NOTES_ON_PIANO; ++i) {
        overpullCents[i] = cents[i];
    }
    setZeroCrossingFreq();
}

float ToneDetector::computeSpinnerAlpha(float dataSumRatio, float threshold) {
    float alpha = 0.0;
    if (dataSumRatio > 1.5F * threshold) {
        alpha = 1.0F;
    } else if (dataSumRatio > 1.2F * threshold) {
        alpha = 0.95F;
    } else if (dataSumRatio > threshold) {
        alpha = 0.9F;
    } else if (dataSumRatio > 0.85F * threshold) {
        alpha = 0.7F;
    } else if (dataSumRatio > 0.75F * threshold) {
        alpha = 0.5F;
    } else if (dataSumRatio > 0.65F * threshold) {
        alpha = 0.2F;
    }
    return alpha;
}

void ToneDetector::setWeights(const double *weights) {
    intervalWeights.set(weights);

    // Convert the peak heights to a new scale related to loudness
    double peakHeightsCorrected[NOTES_ON_PIANO][16];
    // call a method that calculate peakHeightsCorrected based on L values
    calculatePeakHeightsCorrected(peakHeightsSmooth, peakHeightsCorrected);
    tcCalculatorP(centsAve, peakHeightsCorrected, delta);
}

void ToneDetector::getIntervalWidths(double width[32 * NOTES_ON_PIANO], bool useCents) {
    double peakHeightsCorrected[NOTES_ON_PIANO][16];
    calculatePeakHeightsCorrected(peakHeightsSmooth, peakHeightsCorrected);

    IntervalWidths intervalWidths(&pianoKeyFrequencies);
    intervalWidths.computeWidths(peakHeightsCorrected, useCents, width);
}

PianoKeyFrequencies *ToneDetector::getPianoKeyFrequenciesPtr() {
    return &pianoKeyFrequencies;
}


#endif
