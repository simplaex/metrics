package com.simplaex.metrics;

import javax.annotation.Nonnull;

public interface MetricsSender<M extends Metric<M>> {

  void emit(@Nonnull final Metric<M> metric, final double value);

  default void emit(@Nonnull final Metric<M> metric) {
    emit(metric, 1);
  }
}
