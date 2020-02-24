package com.simplaex.metrics;

import lombok.Getter;
import org.junit.Assert;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.util.Arrays;

public class InMemoryMetricsTest {

  enum M implements Metric<M> {
    ONE(Kind.COUNTER),
    TWO(Kind.GAUGE);

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
    Assert.assertEquals(sender.getValues().get("one").stripTrailingZeros(), BigDecimal.valueOf(18).stripTrailingZeros());
    Assert.assertEquals(sender.getValues().get("two").stripTrailingZeros(), BigDecimal.valueOf(19).stripTrailingZeros());
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

}
