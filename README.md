# dogstatsd metrics

This library implements sending statsd metrics with
dogstatsd like tags. Currently the only two supported
metric types are `count` and `gauge` values.

## Maven dependency

`com.simplaex:metrics` is deployed in Maven central as:

```
<dependency>
  <groupId>com.simplaex</groupId>
  <artifactId>metrics</artifactId>
  <version>1.1</version>
</dependency>
```

## How to use

`com.simplaex.Metrics` requires a bit of work defining
the existing metrics that your application emits. This
is on purpose to make it easier to maintain the emitted
metrics as you will have a central definition of all
your metrics that an application emits. That allows you
to, for example, define sample rates for certain metrics
in one place.

```
package com.simplaex.metrics;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

public class UsageExample {

  @RequiredArgsConstructor
  public enum AppMetrics implements Metric<AppMetrics> {
    SOME_METRIC("com.simplaex.request", Kind.COUNTER, 0.1),
    SOME_OTHER_METRIC("com.simplaex.sessions", Kind.GAUGE);

    AppMetrics(final String name, final Kind kind) {
      this(name, kind, 1.0);
    }

    @Getter
    private final String name;

    @Getter
    private final Metric.Kind kind;

    @Getter
    private final double sampleRate;

  }

  public static void main(final String... args) {
    final MetricsSender<AppMetrics> metricsSender = new StatsdMetricSender<>();

    metricsSender.emit(AppMetrics.SOME_METRIC.withTag("path", "/api/v1").withTag("method", "GET"));
  }

}
```

## Kubernetes support

By default statsd metrics will be emitted to `localhost:8125`.
On kubernetes you may want to emit a statsd metrics collector
as a daemon set. In that case expose the UDP port 8125 or your
metrics collector contains as a host port. `com.simplaex:metrics`
will automatically figure out the ip address of the node your
pod is running on and send metrics there.

That way you do not need to send UDP metrics over the network.

## Usage with `vertx-sugar`

`com.simplaex:vertx-sugar` uses this library. To bind the metrics
sender in your module add a provider:

```
  @Provides
  public MetricsSender<MyApplicationMetrics> provideMetrics() {
    return new StatsdMetricSender<>();
  }
```

You can now inject:

```
private final MetricsSender<MyApplicationMetrics> metricsSender;
```

## Example usage in Scala

```
package your.sample.project

import com.simplaex.metrics.Metric
import com.simplaex.metrics.Metric.Kind

import scala.beans.BeanProperty

sealed abstract class ApplicationMetrics(
  @BeanProperty val name: String,
  @BeanProperty val kind: Metric.Kind,
  sampleRate: Double = 1.0
) extends Metric[ApplicationMetrics] {
  
  object JobStarted extends ApplicationMetrics("fountain.started", Kind.COUNTER)

  object JobFinished extends ApplicationMetrics("fountain.finished", Kind.COUNTER)
}
```
