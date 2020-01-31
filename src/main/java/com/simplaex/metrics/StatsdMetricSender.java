package com.simplaex.metrics;

import lombok.extern.log4j.Log4j2;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

@Log4j2
public class StatsdMetricSender<M extends Metric<M>> extends AbstractMetricSender<M> {

  private final BatchedUdpSender udpSender;

  private static String getStatsdHost() {
    final String host = System.getenv("STATSD_HOST");
    if (host != null && !host.isEmpty()) {
      log.info("Using STATSD_HOST environment variable (host={})", host);
      return host;
    }
    try {
      final String hostIp = KubernetesSupport.getHostIp();
      log.info("Using Kubernetes Host-IP={}", hostIp);
      return hostIp;
    } catch (final Exception exc) {
      log.warn("Could not get kubernetes host ip and STATSD_HOST environment variable is not set, falling back to lcoalhost", exc);
      return "localhost";
    }
  }

  private static final int DEFAULT_STATSD_PORT = 8125;

  private static int getStatsdPort() {
    final String envPort = System.getenv("STATSD_PORT");
    if (envPort == null || envPort.isEmpty()) {
      log.info("STATSD_PORT is not set, using default={}", DEFAULT_STATSD_PORT);
      return DEFAULT_STATSD_PORT;
    }
    try {
      final int port = Integer.parseInt(envPort);
      log.info("Using port={} from STATSD_PORT environment variable", port);
      return port;
    } catch (final Exception exc) {
      log.warn("Could not parse STATSD_PORT={} as a port number, falling back to default={}", envPort, DEFAULT_STATSD_PORT);
      return DEFAULT_STATSD_PORT;
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
