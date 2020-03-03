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
  default Metric<M> withTag(@Nonnull final String key, @Nullable final Object value) {
    return new Metric<M>() {

      private final List<Tag> tags = new ArrayList<>(5);

      {
        tags.add(tag(key, value));
      }

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
      public Metric<M> withTag(@Nonnull final String tagName, @Nullable final Object value1) {
        tags.add(tag(tagName, value1));
        return this;
      }

      @Override
      @Nonnull
      public List<Tag> getTags() {
        return tags;
      }
    };
  }

  @Value
  class Tag {
    private final String name;
    private final String value;
  }

  enum Kind {
    GAUGE,
    COUNTER,
    TIMING
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
