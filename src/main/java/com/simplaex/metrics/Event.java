package com.simplaex.metrics;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.simplaex.metrics.Metric.tag;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Event {

  public enum Level {
    INFO,
    WARNING,
    ERROR,
    SUCCESS
  }

  public enum Priority {
    NORMAL,
    LOW
  }

  @Nonnull
  private final String title;

  @Nonnull
  private final String message;

  @Nonnull
  private final Level level;

  @Nonnull
  private Priority priority = Priority.NORMAL;

  private List<Metric.Tag> tags = new ArrayList<>(3);

  public static Event info(@Nonnull final String title, @Nonnull final String message) {
    return new Event(title, message, Level.INFO);
  }

  public static Event info(@Nonnull final Throwable throwable) {
    return new Event(throwable.getClass().getName(), throwable.getMessage(), Level.INFO);
  }

  public static Event error(@Nonnull final String title, @Nonnull final String message) {
    return new Event(title, message, Level.ERROR);
  }

  public static Event error(@Nonnull final Throwable throwable) {
    return new Event(throwable.getClass().getName(), throwable.getMessage(), Level.ERROR);
  }

  public static Event warning(@Nonnull final String title, @Nonnull final String message) {
    return new Event(title, message, Level.WARNING);
  }

  public static Event warning(@Nonnull final Throwable throwable) {
    return new Event(throwable.getClass().getName(), throwable.getMessage(), Level.WARNING);
  }

  public static Event success(@Nonnull final String title, @Nonnull final String message) {
    return new Event(title, message, Level.SUCCESS);
  }

  public Event withTag(final String tag, final Object value) {
    tags.add(tag(tag, value));
    return this;
  }

  public Event lowPriority() {
    this.priority = Priority.LOW;
    return this;
  }

}
