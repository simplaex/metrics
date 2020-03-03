package com.simplaex.metrics;

import lombok.Getter;
import org.junit.Assert;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;

public class InMemoryMetricsTest {

  enum M implements Metric<M> {
    ONE(Kind.COUNTER),
    TWO(Kind.GAUGE),
    TIME(Kind.TIMING);

    M(@Nonnull final Kind kind) {
      this.name = name().toLowerCase();
      this.kind = kind;
    }

    @Getter
    private final String name;

    @Getter
    private final Kind kind;
  }

  @Test
  public void checkInMemoryMetrics() {
    final InMemoryMetrics<M> sender = new InMemoryMetrics<>();
    sender.emit(M.ONE, 7.5);
    sender.emit(M.TWO, 14);
    sender.emit(M.ONE, 10.5);
    sender.emit(M.TWO, 19);
    sender.emit(M.ONE.withTag("key", "value"), 3.75);
    final Map<String, BigDecimal> values = sender.getValues();
    Assert.assertEquals(values.get("one").stripTrailingZeros(), BigDecimal.valueOf(18).stripTrailingZeros());
    Assert.assertEquals(values.get("two").stripTrailingZeros(), BigDecimal.valueOf(19).stripTrailingZeros());
    Assert.assertEquals(values.get("one,key=value").stripTrailingZeros(), BigDecimal.valueOf(3.75).stripTrailingZeros());
  }

  @Test
  public void checkInMemoryEvents() {
    final InMemoryMetrics<M> sender = new InMemoryMetrics<>();
    sender.emit(Event.info("title", "message"));
    sender.emit(Event.warning("label", "description"));
    Assert.assertEquals(sender.getEvents(), Arrays.asList(
      new Event("title", "message", Event.Level.INFO),
      new Event("label", "description", Event.Level.WARNING)
    ));
  }

  @Test
  public void checkInMemoryTimings() {
    final InMemoryMetrics<M> sender = new InMemoryMetrics<>();
    final String result = sender.time(M.TIME, () -> "hello");
    Assert.assertEquals("hello", result);
    Assert.assertTrue(sender.getValues().get("time").compareTo(BigDecimal.ZERO) > 0);
  }

}
