package com.simplaex.metrics;

import javax.annotation.Nonnull;

public interface MetricsSender<M extends Metric<M>> {

  void emit(@Nonnull final Metric<M> metric, final double value);

  default void emit(@Nonnull final Metric<M> metric) {
    emit(metric, 1);
  }

  void emit(@Nonnull final Event event);

  static <M extends Metric<M>> MetricsSender<M> udpSender() {
    return new StatsdMetricSender<>();
  }

  static <M extends Metric<M>> MetricsSender<M> udpSender(final @Nonnull String host) {
    return new StatsdMetricSender<>(host, 8125);
  }

  static <M extends Metric<M>> MetricsSender<M> udpSender(final @Nonnull String host, final @Nonnull int port) {
    return new StatsdMetricSender<>(host, port);
  }

  static <M extends Metric<M>> MetricsSender<M> noMetrics() {
    return new NoMetrics<>();
  }

  static <M extends Metric<M>> InMemoryMetrics<M> inMemoryMetrics() {
    return new InMemoryMetrics<>();
  }
}
