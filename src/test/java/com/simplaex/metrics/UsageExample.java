package com.simplaex.metrics;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

public class UsageExample {

  @RequiredArgsConstructor
  public enum AppMetrics implements Metric<AppMetrics> {
    SOME_METRIC("com.simplaex.request", Kind.COUNTER, 0.1),
    SOME_OTHER_METRIC("com.simplaex.sessions", Kind.GAUGE);

    AppMetrics(final String name, final Kind kind) {
      this(name, kind, 1.0);
    }

    @Getter
    private final String name;

    @Getter
    private final Metric.Kind kind;

    @Getter
    private final double sampleRate;

  }

  public static void main(final String... args) throws InterruptedException {
    final MetricsSender<AppMetrics> metricsSender = new StatsdMetricSender<>();
    for (int i = 0; i < 100; i += 1) {
      metricsSender.emit(AppMetrics.SOME_METRIC.withTag("path", "/api/v1").withTag("method", "GET"));
      metricsSender.emit(Event.info("some info", "some message"));
    }
    Thread.sleep(100);
  }

}
