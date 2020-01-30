package com.simplaex.metrics;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

public class StatsdMetricSender<M extends Metric<M>> extends AbstractMetricSender<M> {

  private final BatchedUdpSender udpSender;

  private static String getStatsdHost() {
    try {
      return KubernetesSupport.getHostIp();
    } catch (final Exception exc) {
      final String host = System.getenv("STATSD_HOST");
      if (host == null) {
        return "localhost";
      }
      return host;
    }
  }

  private static int getStatsdPort() {
    try {
      return Integer.parseInt(System.getenv("STATSD_PORT"));
    } catch (final Exception exc) {
      return 8125;
    }
  }

  public StatsdMetricSender() {
    this(getStatsdHost(), getStatsdPort());
  }

  public StatsdMetricSender(@Nonnull final String host, @Nonnegative final int port) {
    super(new DogstatsdMetricBuilder());
    this.udpSender = new BatchedUdpSender(host, port);
  }

  @Override
  protected void send(@Nonnull final String metric) {
    udpSender.send(metric);
  }
}
