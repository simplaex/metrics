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
      case TIMING:
        b.append("ms");
        break;
    }
    if (sampleRate < 1.0 && sampleRate > 0) {
      b.append("|@");
      b.append(sampleRate);
    }
    appendTags(b, tags);
    b.append('\n');
    return b.toString();
  }

  private static void appendTags(@Nonnull final StringBuilder b, @Nonnull final Iterable<Metric.Tag> tags) {
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
    }
  }

  @Nonnull
  private String escapeEventString(@Nonnull final String string) {
    return string.replaceAll("\n", "\\n");
  }

  @Nonnull
  @Override
  public String build(@Nonnull final Event event) {
    final String title = escapeEventString(event.getTitle());
    final String message = escapeEventString(event.getMessage());
    final StringBuilder b = new StringBuilder("_e{");
    b.append(title.length());
    b.append(',');
    b.append(message.length());
    b.append("}:");
    b.append(title);
    b.append('|');
    b.append(message);
    if (event.getPriority() != Event.Priority.NORMAL) {
      b.append("|p:");
      b.append(event.getPriority().toString().toLowerCase());
    }
    if (event.getLevel() != Event.Level.INFO) {
      b.append("|t:");
      b.append(event.getLevel().toString().toLowerCase());
    }
    appendTags(b, event.getTags());
    b.append('\n');
    return b.toString();
  }

}
