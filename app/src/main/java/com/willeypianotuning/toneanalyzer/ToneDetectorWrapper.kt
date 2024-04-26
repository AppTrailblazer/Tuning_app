package com.willeypianotuning.toneanalyzer

import com.willeypianotuning.toneanalyzer.audio.*
import com.willeypianotuning.toneanalyzer.audio.enums.PitchRaiseMode
import com.willeypianotuning.toneanalyzer.extensions.flatten
import com.willeypianotuning.toneanalyzer.extensions.reshape
import com.willeypianotuning.toneanalyzer.store.db.tuning_styles.IntervalWeights
import com.willeypianotuning.toneanalyzer.store.db.tunings.PianoTuningMeasurements
import timber.log.Timber

class ToneDetectorWrapper private constructor() : AutoCloseable {
    var pitchRaiseOptions: PitchRaiseOptions? = null
        private set

    val pitchRaiseData = PitchRaiseData()

    @JvmField
    val pitchRaiseModeLock = Any()

    private var toneAnalyzerPtr: Long = createToneDetectorNative()

    val pianoKeyFrequenciesPtr: Long = getPianoKeyFrequenciesPtr(toneAnalyzerPtr)

    fun startPitchRaiseMeasurement(options: PitchRaiseOptions) {
        options.measurement = MeasurementData(fx, harmonics)
        options.mode = PitchRaiseMode.MEASUREMENT
        pitchRaiseOptions = options
        fx = DoubleArray(88)
        harmonics = Array(88) { DoubleArray(10) }
        startPitchRaiseMeasurement(options.keys)
    }

    fun startPitchRaiseTuning() {
        val pitchRaiseOptions = pitchRaiseOptions
        if (pitchRaiseOptions == null || pitchRaiseOptions.mode != PitchRaiseMode.MEASUREMENT) {
            Timber.w("PitchRaise measurement is not active")
            return
        }
        stopPitchRaiseMeasurement()
        pitchRaiseOptions.mode = PitchRaiseMode.TUNING
        val measurementData = pitchRaiseOptions.measurement
        if (measurementData != null) {
            fx = measurementData.fx
            harmonics = measurementData.harmonics
        } else {
            Timber.w("PitchRaise: no initial measurement data")
        }
    }

    fun stopPitchRaise() {
        stopPitchRaiseMeasurement()
        pitchRaiseOptions = null
    }

    val pitchRaiseMode: Int
        get() = pitchRaiseOptions?.mode ?: PitchRaiseMode.OFF

    override fun close() {
        destroyNativeClasses(toneAnalyzerPtr)
        toneAnalyzerPtr = 0L
    }

    protected fun finalize() {
        destroyNativeClasses(toneAnalyzerPtr)
        toneAnalyzerPtr = 0L
    }

    fun addData(buffer: ShortArray, bufferLen: Int) {
        addDataNative(toneAnalyzerPtr, buffer, bufferLen)
    }

    fun setCurrentNoteRelative(note: Int) {
        setCurrentNoteNative(toneAnalyzerPtr, note, true)
    }

    var harmonics: Array<DoubleArray>
        get() {
            val temp = DoubleArray(88 * 10)
            getHarmonicsNative(toneAnalyzerPtr, temp)
            return temp.reshape(88, 10)
        }
        set(value) {
            setHarmonicsNative(toneAnalyzerPtr, value.flatten())
        }

    val bx: IntArray
        get() {
            val temp = IntArray(88)
            val len = getBxNative(toneAnalyzerPtr, temp)
            return temp.copyOfRange(0, len)
        }

    val by: DoubleArray
        get() {
            val temp = DoubleArray(88)
            val len = getByNative(toneAnalyzerPtr, temp)
            return temp.copyOfRange(0, len)
        }

    val bxfit: DoubleArray
        get() {
            val bxfit = DoubleArray(4)
            getBxfitNative(toneAnalyzerPtr, bxfit)
            return bxfit
        }

    val bave: DoubleArray
        get() {
            val Bave = DoubleArray(88)
            getBaveNative(toneAnalyzerPtr, Bave)
            return Bave
        }

    var fx: DoubleArray
        get() {
            val fx = DoubleArray(88)
            getFxNative(toneAnalyzerPtr, fx)
            return fx
        }
        set(value) {
            setFxNative(toneAnalyzerPtr, value)
        }

    val delta: DoubleArray
        get() {
            val delta = DoubleArray(88)
            getDeltaNative(toneAnalyzerPtr, delta)
            return delta
        }

    val fftResArray: DoubleArray
        get() {
            val fftres = DoubleArray(2048)
            val temp = FloatArray(2048)
            getFFTResArrayNative(toneAnalyzerPtr, temp)
            for (i in 0..2047) {
                fftres[i] = temp[i].toDouble()
            }
            return fftres
        }

    val num: Float
        get() = getNumNative(toneAnalyzerPtr)

    val spinnerEnabled: BooleanArray
        get() = getSpinnerEnabledNative(toneAnalyzerPtr)

    val targetPeakFrequencies: DoubleArray
        get() = getTargetPeakFrequenciesNative(toneAnalyzerPtr)

    val angle: Float
        get() = getAngleNative(toneAnalyzerPtr)

    val centsOffsetAvg: Float
        get() = getCentsOffsetAvgNative(toneAnalyzerPtr)

    val centsOffsetZCAvg: Float
        get() = getCentsOffsetZCAvgNative(toneAnalyzerPtr)

    val centsOffsetCombined: Float
        get() = getCentsOffsetCombinedNative(toneAnalyzerPtr)

    var currentNote: Int
        get() = getCurrentNoteNative(toneAnalyzerPtr)
        set(note) {
            setCurrentNoteNative(toneAnalyzerPtr, note, false)
        }

    /**
     * Used for anti-tampering dialog
     *
     * @return number of note switches (Note Switching Counter)
     */
    val nsc: Int
        get() = getNSCNative(toneAnalyzerPtr)

    val candidateNote: Int
        get() = getCandidateNoteNative(toneAnalyzerPtr)

    val acNote: Int
        get() = getAcToneNative(toneAnalyzerPtr)

    val offsetOver: Boolean
        get() = getOffsetOverNative(toneAnalyzerPtr)

    fun getPhaseAndAlpha(partial: Int): FloatArray {
        return getPhaseAndAlphaNative(toneAnalyzerPtr, partial)
    }

    fun skipPhases(partial: Int, skip: Int) {
        skipPhasesNative(toneAnalyzerPtr, partial, skip)
    }

    fun setOverpullCents(overpullCents: DoubleArray?) {
        var cents = overpullCents
        if (cents == null) {
            cents = DoubleArray(88)
        }
        setOverpullCentsNative(toneAnalyzerPtr, cents)
    }

    fun processFrame(): Boolean {
        return processFrameNative(toneAnalyzerPtr)
    }

    fun detectNotes(): Boolean {
        return detectNotesNative(toneAnalyzerPtr)
    }

    fun processZeroCrossing() {
        processZeroCrossingNative(toneAnalyzerPtr)
    }

    val isQualityTestOk: Boolean
        get() = isQualityTestOkNative(toneAnalyzerPtr)

    val isTargetLengthCounted: Boolean
        get() = isTargetLengthCountedNative(toneAnalyzerPtr)

    fun setRecalculateTuning(recalculate: Boolean) {
        setRecalculateTuningNative(toneAnalyzerPtr, recalculate)
    }

    fun forceRecalculate() {
        forceRecalculateNative(toneAnalyzerPtr)
    }

    fun reset() {
        resetNative(toneAnalyzerPtr)
    }

    var noteDetectMode: Int
        get() = getNoteChangeModeNative(toneAnalyzerPtr)
        set(mode) {
            setNoteChangeModeNative(toneAnalyzerPtr, mode)
        }

    var inharmonicity: Array<DoubleArray>
        get() {
            val temp = DoubleArray(88 * 3)
            getInharmonicityNative(toneAnalyzerPtr, temp)
            return temp.reshape(88, 3)
        }
        set(value) {
            setInharmonicityNative(toneAnalyzerPtr, value.flatten())
        }

    val peaksHeight: Array<DoubleArray>
        get() {
            val temp = DoubleArray(88 * 16)
            getPeaksHeightNative(toneAnalyzerPtr, temp)
            return temp.reshape(88, 16)
        }

    fun setData(measurements: PianoTuningMeasurements) {
        setDataNative(
            toneAnalyzerPtr,
            measurements.peakHeights.flatten(),
            measurements.inharmonicity.flatten(),
            measurements.delta,
            measurements.bxFit,
            measurements.fx,
            measurements.harmonics.flatten()
        )
    }

    fun setTemperament(temperament: DoubleArray) {
        setTemperamentNative(toneAnalyzerPtr, temperament)
    }

    fun runTcCalculator() {
        runTcCalculatorNative(toneAnalyzerPtr)
    }

    private fun startPitchRaiseMeasurement(keys: IntArray) {
        startPitchRaiseMeasurementNative(toneAnalyzerPtr, keys)
    }

    fun setIntervalWeights(weights: IntervalWeights) {
        setIntervalWeightsNative(toneAnalyzerPtr, weights.joined())
    }

    val defaultIntervalWeights: IntervalWeights
        get() {
            val temp = DoubleArray(20)
            getDefaultIntervalWeightsNative(toneAnalyzerPtr, temp)
            val octave = temp.copyOfRange(0, 5)
            val twelfth = temp.copyOfRange(5, 8)
            val doubleOctave = temp.copyOfRange(8, 10)
            val nineteenth = temp.copyOfRange(10, 11)
            val tripleOctave = temp.copyOfRange(11, 12)
            val fifth = temp.copyOfRange(12, 14)
            val fourth = temp.copyOfRange(14, 16)
            val extraTrebleStretch = temp.copyOfRange(16, 18)
            val extraBassStretch = temp.copyOfRange(18, 20)
            return IntervalWeights(
                octave,
                twelfth,
                doubleOctave,
                nineteenth,
                tripleOctave,
                fifth,
                fourth,
                extraTrebleStretch,
                extraBassStretch
            )
        }

    private fun stopPitchRaiseMeasurement() {
        stopPitchRaiseMeasurementNative(toneAnalyzerPtr)
    }

    val isPitchRaiseMeasurementOn: Boolean
        get() = isPitchRaiseMeasurementOnNative(toneAnalyzerPtr)

    var calibrationFactor: Double
        get() = getCalibrationFactorNative(toneAnalyzerPtr)
        set(factor) {
            setCalibrationFactorNative(toneAnalyzerPtr, factor)
        }

    fun setInharmonicityWeight(inharmonicityWeight: Double) {
        setInharmonicityWeightNative(toneAnalyzerPtr, inharmonicityWeight)
    }

    fun setInputSamplingRate(ratio: Double) {
        setInputSamplingRateNative(toneAnalyzerPtr, ratio)
    }

    fun getIntervalWidths(useCents: Boolean): IntervalWidth {
        val width = getIntervalWidthsNative(toneAnalyzerPtr, useCents)
        return IntervalWidth(
            IntervalWidthData(
                width.copyOfRange(0, 5 * 88).reshape(5, 88),
                width.copyOfRange(5 * 88, 10 * 88).reshape(5, 88),
            ),
            IntervalWidthData(
                width.copyOfRange(10 * 88, 12 * 88).reshape(2, 88),
                width.copyOfRange(12 * 88, 14 * 88).reshape(2, 88),
            ),
            IntervalWidthData(
                width.copyOfRange(14 * 88, 16 * 88).reshape(2, 88),
                width.copyOfRange(16 * 88, 18 * 88).reshape(2, 88),
            ),
            IntervalWidthData(
                width.copyOfRange(18 * 88, 21 * 88).reshape(3, 88),
                width.copyOfRange(21 * 88, 24 * 88).reshape(3, 88),
            ),
            IntervalWidthData(
                width.copyOfRange(24 * 88, 26 * 88).reshape(2, 88),
                width.copyOfRange(26 * 88, 28 * 88).reshape(2, 88),
            ),
            IntervalWidthData(
                width.copyOfRange(28 * 88, 29 * 88).reshape(1, 88),
                width.copyOfRange(29 * 88, 30 * 88).reshape(1, 88),
            ),
            IntervalWidthData(
                width.copyOfRange(30 * 88, 31 * 88).reshape(1, 88),
                width.copyOfRange(31 * 88, 32 * 88).reshape(1, 88),
            )
        )
    }

    var pitchOffsetFactor: Double
        get() = getPitchOffsetFactorNative(toneAnalyzerPtr)
        set(factor) {
            setPitchOffsetFactorNative(toneAnalyzerPtr, factor)
        }

    fun resetNSC() {
        setNSCNative(toneAnalyzerPtr, 0)
    }

    private external fun createToneDetectorNative(): Long
    private external fun destroyNativeClasses(instance: Long)
    private external fun addDataNative(instance: Long, buffer: ShortArray, bufferLen: Int)
    private external fun getBxNative(instance: Long, buffer: IntArray): Int
    private external fun getByNative(instance: Long, buffer: DoubleArray): Int
    private external fun getBxfitNative(instance: Long, buffer: DoubleArray): Int
    private external fun getBaveNative(instance: Long, buffer: DoubleArray): Int
    private external fun getDeltaNative(instance: Long, buffer: DoubleArray): Int
    private external fun getFxNative(instance: Long, buffer: DoubleArray): Int
    private external fun setFxNative(instance: Long, buffer: DoubleArray)
    private external fun getFFTResArrayNative(instance: Long, buffer: FloatArray): Int
    private external fun setInharmonicityNative(instance: Long, buffer: DoubleArray)
    private external fun getHarmonicsNative(instance: Long, buffer: DoubleArray): Int
    private external fun setHarmonicsNative(instance: Long, buffer: DoubleArray)
    private external fun setNoteChangeModeNative(instance: Long, mode: Int)
    private external fun getNoteChangeModeNative(instance: Long): Int
    private external fun getInharmonicityNative(instance: Long, buffer: DoubleArray): Int
    private external fun getPeaksHeightNative(instance: Long, buffer: DoubleArray): Int
    private external fun getDefaultIntervalWeightsNative(instance: Long, buffer: DoubleArray): Int
    private external fun setDataNative(
        instance: Long,
        peaksheight: DoubleArray,
        inharmonicity: DoubleArray,
        delta: DoubleArray,
        bxfit: DoubleArray,
        fx: DoubleArray,
        harmonics: DoubleArray
    )

    private external fun setTemperamentNative(instance: Long, temperament: DoubleArray)
    private external fun runTcCalculatorNative(instance: Long)
    private external fun setIntervalWeightsNative(instance: Long, weights: DoubleArray)
    private external fun getNumNative(instance: Long): Float
    private external fun getSpinnerEnabledNative(instance: Long): BooleanArray
    private external fun getTargetPeakFrequenciesNative(instance: Long): DoubleArray
    private external fun getAngleNative(instance: Long): Float
    private external fun getCentsOffsetAvgNative(instance: Long): Float
    private external fun getCentsOffsetZCAvgNative(instance: Long): Float
    private external fun getCentsOffsetCombinedNative(instance: Long): Float
    private external fun getCurrentNoteNative(instance: Long): Int
    private external fun getCandidateNoteNative(instance: Long): Int
    private external fun getNSCNative(instance: Long): Int
    private external fun getAcToneNative(instance: Long): Int
    private external fun getOffsetOverNative(instance: Long): Boolean
    private external fun getPhaseAndAlphaNative(instance: Long, partial: Int): FloatArray
    private external fun skipPhasesNative(instance: Long, partial: Int, skip: Int)
    private external fun processFrameNative(instance: Long): Boolean
    private external fun detectNotesNative(instance: Long): Boolean
    private external fun processZeroCrossingNative(instance: Long)
    private external fun setCurrentNoteNative(instance: Long, note: Int, isrelative: Boolean)
    private external fun setNSCNative(instance: Long, value: Int)
    private external fun isQualityTestOkNative(instance: Long): Boolean
    private external fun isTargetLengthCountedNative(instance: Long): Boolean
    private external fun setRecalculateTuningNative(instance: Long, isset: Boolean)
    private external fun forceRecalculateNative(instance: Long)
    private external fun resetNative(instance: Long)
    private external fun startPitchRaiseMeasurementNative(instance: Long, keys: IntArray)
    private external fun stopPitchRaiseMeasurementNative(instance: Long)
    private external fun isPitchRaiseMeasurementOnNative(instance: Long): Boolean
    private external fun setCalibrationFactorNative(instance: Long, factor: Double)
    private external fun setOverpullCentsNative(instance: Long, buffer: DoubleArray)
    private external fun getIntervalWidthsNative(instance: Long, useCents: Boolean): DoubleArray
    private external fun getCalibrationFactorNative(instance: Long): Double
    private external fun setPitchOffsetFactorNative(instance: Long, factor: Double)
    private external fun getPitchOffsetFactorNative(instance: Long): Double
    private external fun setInharmonicityWeightNative(instance: Long, inHarmonicityWeight: Double)
    private external fun setInputSamplingRateNative(instance: Long, factor: Double)
    private external fun getPianoKeyFrequenciesPtr(instance: Long): Long

    companion object {
        const val FFT_SIZE = 4096
        const val SAMPLE_FREQ = 16000
        const val DEFAULT_NOTE = 49 // A4

        fun newInstance(): ToneDetectorWrapper {
            return ToneDetectorWrapper()
        }

        init {
            System.loadLibrary("WilleyToneAnalyzerLib")
        }
    }
}