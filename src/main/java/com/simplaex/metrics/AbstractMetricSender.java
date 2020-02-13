package com.simplaex.metrics;

import javax.annotation.Nonnull;

public abstract class AbstractMetricSender<M extends Metric<M>> implements MetricsSender<M> {

  private final MetricBuilder metricBuilder;

  protected AbstractMetricSender(final MetricBuilder metricBuilder) {
    this.metricBuilder = metricBuilder;
  }

  abstract protected void send(final String metric);

  @Override
  public void emit(@Nonnull final Metric<M> metric, final double value) {
    final double sampleRate = metric.getSampleRate();
    if (sampleRate >= 1.0 || sampleRate < 1.0 && Math.random() < sampleRate) {
      final String line = metricBuilder.build(metric.getName(), metric.getKind(), metric.getSampleRate(), value, metric.getTags());
      send(line);
    }
  }

  @Override
  public void emit(@Nonnull final Event event) {
    send(metricBuilder.build(event));
  }
}
