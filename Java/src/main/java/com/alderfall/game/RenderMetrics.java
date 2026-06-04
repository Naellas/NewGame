package com.alderfall.game;

import java.util.LinkedHashMap;
import java.util.Map;

final class RenderMetrics {
    private static final String ENABLED_PROPERTY = "alderfall.renderMetrics";
    private static final String REPORT_EVERY_PROPERTY = "alderfall.renderMetricsEvery";

    private final boolean enabled;
    private final int reportEveryFrames;
    private final Map<String, Metric> metrics = new LinkedHashMap<>();
    private final Map<String, SampleMetric> samples = new LinkedHashMap<>();
    private int sampledFrames;

    private RenderMetrics(boolean enabled, int reportEveryFrames) {
        this.enabled = enabled;
        this.reportEveryFrames = Math.max(1, reportEveryFrames);
    }

    static RenderMetrics create() {
        return new RenderMetrics(Boolean.getBoolean(ENABLED_PROPERTY),
                Integer.getInteger(REPORT_EVERY_PROPERTY, 180));
    }

    boolean enabled() {
        return enabled;
    }

    long start() {
        return enabled ? System.nanoTime() : 0L;
    }

    void record(String name, long startedNanos) {
        if (!enabled || startedNanos == 0L) {
            return;
        }
        metrics.computeIfAbsent(name, ignored -> new Metric()).add(System.nanoTime() - startedNanos);
    }

    void sample(String name, long value) {
        if (!enabled) {
            return;
        }
        samples.computeIfAbsent(name, ignored -> new SampleMetric()).add(value);
    }

    void endFrame(int frame) {
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
                    .append(DebugMetrics.millis(metric.averageNanos()));
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
        private int samples;

        void add(long nanos) {
            totalNanos += nanos;
            samples++;
        }

        long averageNanos() {
            return samples == 0 ? 0L : totalNanos / samples;
        }

        void reset() {
            totalNanos = 0L;
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
