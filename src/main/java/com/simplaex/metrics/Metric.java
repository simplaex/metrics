package com.simplaex.metrics;

import lombok.Value;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public interface Metric<M extends Metric> {

  @Nonnull
  String getName();

  @Nonnull
  Kind getKind();

  @Nonnegative
  default double getSampleRate() {
    return 1.0;
  }

  @Nonnull
  default M withTag(@Nonnull final String key, @Nullable final Object value) {
    @SuppressWarnings("unchecked") final M metric = (M) new Metric<M>() {

      private final List<Tag> tags = new ArrayList<>(5);

      @Nonnull
      @Override
      public String getName() {
        return Metric.this.getName();
      }

      @Nonnull
      @Override
      public Kind getKind() {
        return Metric.this.getKind();
      }

      @Nonnegative
      @Override
      public double getSampleRate() {
        return Metric.this.getSampleRate();
      }

      @Override
      @Nonnull
      public M withTag(@Nonnull final String tagName, @Nullable final Object value) {
        tags.add(tag(tagName, value));
        @SuppressWarnings("unchecked") final M thiz = (M) this;
        return thiz;
      }

      @Override
      @Nonnull
      public List<Tag> getTags() {
        return tags;
      }
    };
    return metric;
  }

  @Value
  class Tag {
    private final String name;
    private final String value;
  }

  enum Kind {
    GAUGE,
    COUNTER
  }

  static Tag tag(@Nonnull final String tagName, @Nullable final Object value) {
    Objects.requireNonNull(tagName, "'tagName' must not be null");
    return new Tag(tagName, Objects.toString(value));
  }

  @Nonnull
  default Collection<Tag> getTags() {
    return Collections.emptyList();
  }
}
