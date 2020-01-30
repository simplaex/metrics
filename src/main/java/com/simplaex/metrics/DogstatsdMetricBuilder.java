package com.simplaex.metrics;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Iterator;

public class DogstatsdMetricBuilder implements MetricBuilder {

  @Nonnull
  @Override
  public String build(
    @Nonnull final String metricName,
    @Nonnull final Metric.Kind kind,
    final double sampleRate,
    final double value,
    @Nonnull final Collection<Metric.Tag> tags
  ) {
    final StringBuilder b = new StringBuilder();
    b.append(metricName);
    b.append(':');
    b.append(value);
    b.append('|');
    switch (kind) {
      case GAUGE:
        b.append('g');
        break;
      case COUNTER:
        b.append('c');
        break;
    }
    if (sampleRate < 1.0 && sampleRate > 0) {
      b.append("|@");
      b.append(sampleRate);
    }
    final Iterator<Metric.Tag> tagIterator = tags.iterator();
    if (tagIterator.hasNext()) {
      final Metric.Tag tag = tagIterator.next();
      b.append("|#");
      b.append(tag.getName());
      b.append(':');
      b.append(tag.getValue());
      while (tagIterator.hasNext()) {
        final Metric.Tag next = tagIterator.next();
        b.append(',');
        b.append(next.getName());
        b.append(':');
        b.append(next.getValue());
      }
      b.append('\n');
    }
    return b.toString();
  }

}
