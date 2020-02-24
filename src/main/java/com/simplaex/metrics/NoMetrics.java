package com.simplaex.metrics;

import javax.annotation.Nonnull;
import java.util.Objects;

public class NoMetrics<M extends Metric<M>> implements MetricsSender<M> {
  @Override
  public void emit(@Nonnull final Metric<M> metric, final double value) {
    Objects.requireNonNull(metric, "'metric' must not be null");
  }

  @Override
  public void emit(@Nonnull final Event event) {
    Objects.requireNonNull(event, "'event' must not be null");
  }
}
