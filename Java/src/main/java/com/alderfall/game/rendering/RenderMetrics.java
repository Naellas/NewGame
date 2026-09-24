package com.alderfall.game;

import java.util.LinkedHashMap;
import java.util.Map;

public final class RenderMetrics {
    private static final String ENABLED_PROPERTY = "alderfall.renderMetrics";
    private static final String REPORT_EVERY_PROPERTY = "alderfall.renderMetricsEvery";

    private final boolean enabled;
    private boolean overlayEnabled;
    private final Map<String, Long> overlayTimings = new LinkedHashMap<>();
    private final int reportEveryFrames;
    private final Map<String, Metric> metrics = new LinkedHashMap<>();
    private final Map<String, SampleMetric> samples = new LinkedHashMap<>();
    private int sampledFrames;

    private RenderMetrics(boolean enabled, int reportEveryFrames) {
        this.enabled = enabled;
        this.reportEveryFrames = Math.max(1, reportEveryFrames);
    }

    public static RenderMetrics create() {
        return new RenderMetrics(Boolean.getBoolean(ENABLED_PROPERTY),
                Integer.getInteger(REPORT_EVERY_PROPERTY, 180));
    }

    public boolean enabled() {
        return enabled || overlayEnabled;
    }

    public long start() {
        return enabled() ? System.nanoTime() : 0L;
    }

    public void record(String name, long startedNanos) {
        if (!enabled() || startedNanos == 0L) {
            return;
        }
        long elapsed = System.nanoTime() - startedNanos;
        if (overlayEnabled) overlayTimings.merge(name, elapsed, Long::sum);
        if (enabled) metrics.computeIfAbsent(name, ignored -> new Metric()).add(elapsed);
    }

    public void setOverlayEnabled(boolean active) {
        overlayEnabled = active;
        overlayTimings.clear();
    }

    public Map<String, Long> takeOverlayTimings() {
        if (!overlayEnabled) return Map.of();
        Map<String, Long> result = Map.copyOf(overlayTimings);
        overlayTimings.clear();
        return result;
    }

    public void sample(String name, long value) {
        if (!enabled) {
            return;
        }
        samples.computeIfAbsent(name, ignored -> new SampleMetric()).add(value);
    }

    public void endFrame(int frame) {
        if (!enabled) {
            return;
        }
        sampledFrames++;
        if (sampledFrames < reportEveryFrames) {
            return;
        }
        StringBuilder line = new StringBuilder("renderMetrics frame=").append(frame);
        for (Map.Entry<String, Metric> entry : metrics.entrySet()) {
            Metric metric = entry.getValue();
            line.append(' ')
                    .append(entry.getKey())
                    .append('=')
                    .append(DebugMetrics.millis(metric.averageNanos()))
                    .append(' ')
                    .append(entry.getKey())
                    .append(".max=")
                    .append(DebugMetrics.millis(metric.maxNanos));
            metric.reset();
        }
        for (Map.Entry<String, SampleMetric> entry : samples.entrySet()) {
            SampleMetric sample = entry.getValue();
            line.append(' ')
                    .append(entry.getKey())
                    .append(".avg=")
                    .append(sample.average())
                    .append(' ')
                    .append(entry.getKey())
                    .append(".max=")
                    .append(sample.max());
            sample.reset();
        }
        System.out.println(line);
        sampledFrames = 0;
    }

    private static final class Metric {
        private long totalNanos;
        private long maxNanos;
        private int samples;

        void add(long nanos) {
            totalNanos += nanos;
            maxNanos = Math.max(maxNanos, nanos);
            samples++;
        }

        long averageNanos() {
            return samples == 0 ? 0L : totalNanos / samples;
        }

        void reset() {
            totalNanos = 0L;
            maxNanos = 0L;
            samples = 0;
        }
    }

    private static final class SampleMetric {
        private long total;
        private long max;
        private int samples;

        void add(long value) {
            total += value;
            max = samples == 0 ? value : Math.max(max, value);
            samples++;
        }

        long average() {
            return samples == 0 ? 0L : Math.round(total / (double) samples);
        }

        long max() {
            return max;
        }

        void reset() {
            total = 0L;
            max = 0L;
            samples = 0;
        }
    }
}
