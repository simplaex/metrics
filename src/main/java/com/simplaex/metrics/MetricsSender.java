package com.simplaex.metrics;

import javax.annotation.Nonnull;
import java.util.concurrent.Callable;

public interface MetricsSender<M extends Metric<M>> {

  void emit(@Nonnull final Metric<M> metric, final double value);

  default void emit(@Nonnull final Metric<M> metric) {
    emit(metric, 1.0);
  }

  default <V> V time(@Nonnull final Metric<M> metric, final Callable<V> callable) {
    final long started = System.nanoTime();
    try {
      return callable.call();
    } catch (final Exception exc) {
      throw new RuntimeException(exc);
    } finally {
      final long now = System.nanoTime();
      final double diff = (now - started) / 1000.0 /* micros */ / 1000.0 /* millis */;
      emit(metric, diff);
    }
  }

  default void time(@Nonnull final Metric<M> metric, final Runnable runnable) {
    time(metric, () -> {
      runnable.run();
      return null;
    });
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
