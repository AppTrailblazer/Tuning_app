package com.willeypianotuning.toneanalyzer.spinners;

import java.util.ArrayDeque;
import java.util.Queue;

public class Spinner {
    private long ptr;
    private int id;
    private double frequency;
    private String label = "";
    private int divisions;
    private boolean enabled = true;

    private static final int QUEUE_CAPACITY = 6;
    private Queue<Float> phaseQueue = new ArrayDeque<>(QUEUE_CAPACITY);

    public static final float EMPTY = -1.0f;
    public static final float INACTIVE = -2.0f;


    /**
     * Initialize Spinner
     *
     * @param sampleRate     the sample rate
     * @param updateInterval the time between phase updates in milliseconds
     * @param bpfBandwidth   the bandpass filter bandwidth
     */
    public Spinner(int id, int sampleRate, int updateInterval, double bpfBandwidth) {
        this.id = id;
        int samplesPerUpdate = sampleRate * updateInterval / 1000;
        ptr = allocateNative(id, sampleRate, samplesPerUpdate, bpfBandwidth);
        setFrequency(440.0);
    }

    /**
     * Check if this spinner is enabled
     *
     * @return true if enabled, false otherwise
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Set the enabled state of this spinner
     *
     * @param enabled true if enabled, false otherwise
     */
    public synchronized void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            phaseQueue.clear();
        }
    }

    /**
     * Set the reference frequency
     *
     * @param frequency the reference frequency
     */
    public void setFrequency(double frequency) {
        if (this.frequency != frequency) {
            setFrequencyNative(ptr, frequency);
            this.frequency = frequency;
        }
    }

    /**
     * Set the detection parameters for enabling/disabling the spinner
     *
     * @param threshold the amplitude ratio threshold
     */
    public void setDetectionParam(double threshold) {
        setDetectionParamNative(ptr, threshold);
    }

    /**
     * Get the label for this spinner
     *
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * Set the label for this spinner
     *
     * @param label the label
     */
    public void setLabel(String label) {
        this.label = label;
    }

    public int getDivisions() {
        return divisions;
    }

    public void setDivisions(int divisions) {
        this.divisions = divisions;
    }

    /**
     * Processes audio samples then enqueues the phase difference estimates
     *
     * @param samples the short array containing the audio samples
     */
    public void process(short[] samples) {
        float[] phase = getPhaseNative(ptr, samples);
        if (phase != null) {
            synchronized (this) {
                int newQueueSize = phaseQueue.size() + phase.length;
                if (newQueueSize > QUEUE_CAPACITY) {
                    int numberOfItemsToRemove = newQueueSize - QUEUE_CAPACITY;
                    float[] removed = new float[numberOfItemsToRemove];
                    for (int i = 0; i < numberOfItemsToRemove; i++) {
                        Float removedPhase = phaseQueue.poll();
                        if (removedPhase != null) {
                            removed[i] = removedPhase;
                        }
                    }
                }
                for (float p : phase) {
                    if (phaseQueue.size() == QUEUE_CAPACITY) {
                        phaseQueue.poll();
                    }
                    phaseQueue.add(p);
                }
            }
        }
    }

    /**
     * Remove phase estimates from queue
     *
     * @param n the number of elements to remove
     */
    public synchronized void skip(int n) {
        // we don't want the queue to be empty due do skipped frames
        // if we need to skip more frames than the queue size, we still keep the last available phase value
        n = Math.min(n, phaseQueue.size() - 1);
        while (n-- > 0) {
            Float removedItem = phaseQueue.poll();
            if (removedItem == null) {
                break;
            }
        }
    }

    /**
     * Get a phase estimate from the queue
     *
     * @return the phase estimate between 0..1, Spinner.QUEUE_EMPTY if queue is empty,
     * or Spinner.INACTIVE if spinner is not active
     */
    public synchronized float getPhase() {
        Float phase = phaseQueue.poll();
        if (phase != null) {
            return phase;
        } else {
            return EMPTY;
        }
    }

    public synchronized void addPhase(float phase) {
        if (phaseQueue.size() == QUEUE_CAPACITY) {
            phaseQueue.poll();
        }
        phaseQueue.add(phase);
    }

    protected void finalize() throws Throwable {
        freeNative(ptr);
        super.finalize();
    }

    private native long allocateNative(int id, int sampleRate, int samplesPerUpdate, double bpfBandwidth);

    private native void freeNative(long ptr);

    private native void setFrequencyNative(long ptr, double frequency);

    private native void setDetectionParamNative(long ptr, double threshold);

    private native float[] getPhaseNative(long ptr, short[] samples);

    static {
        System.loadLibrary("WilleyToneAnalyzerLib");
    }
}
