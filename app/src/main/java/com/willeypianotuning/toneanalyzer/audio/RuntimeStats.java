package com.willeypianotuning.toneanalyzer.audio;

import android.os.SystemClock;

import timber.log.Timber;

class RuntimeStats {
    private final String label;
    private final int interval;
    private long last = 0;
    private long startTime;
    private long min = Long.MAX_VALUE, max = 0;

    public RuntimeStats(String label, int interval) {
        this.label = label;
        this.interval = interval;
    }

    public void start() {
        startTime = SystemClock.uptimeMillis();
    }

    public void stop() {
        long now = SystemClock.uptimeMillis();
        long duration = now - startTime;
        if (duration >= 2) {  // ignore very short durations
            if (duration < min) {
                min = duration;
            }
            if (duration > max) {
                max = duration;
            }
        }
        if (now - last >= 1000 * interval) {
            last = now;
            if (min == Long.MAX_VALUE) {
                // there were no large data processing intervals, don't write log
                return;
            }
            Timber.d(label + " min " + min + " max " + max + " ms");
            min = Long.MAX_VALUE;
            max = 0;
        }
    }
}