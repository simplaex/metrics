package com.simplaex.metrics;

import javax.annotation.Nonnull;
import java.util.Collection;

public interface MetricBuilder {
  @Nonnull
  String build(
    @Nonnull String metricName,
    @Nonnull Metric.Kind kind,
    double sampleRate,
    double value,
    @Nonnull Collection<Metric.Tag> tags
  );
}
