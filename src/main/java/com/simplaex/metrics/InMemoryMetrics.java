package com.simplaex.metrics;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryMetrics<M extends Metric<M>> implements MetricsSender<M> {

  private final Map<String, BigDecimal> values = new ConcurrentHashMap<>();

  private final List<Event> events = Collections.synchronizedList(new ArrayList<>());

  public Map<String, BigDecimal> getValues() {
    return Collections.unmodifiableMap(values);
  }

  public List<Event> getEvents() {
    return Collections.unmodifiableList(events);
  }

  @Override
  public void emit(@Nonnull final Metric<M> metric, final double value) {
    switch (metric.getKind()) {
      case GAUGE:
        values.put(metric.getName(), BigDecimal.valueOf(value));
        break;
      case COUNTER:
        values.compute(metric.getName(), (k, v) -> (v == null ? BigDecimal.ZERO : v).add(BigDecimal.valueOf(value)));
        break;
    }
  }

  @Override
  public void emit(@Nonnull final Event event) {
    events.add(event);
  }

  public void reset() {
    values.clear();
    events.clear();
  }
}
