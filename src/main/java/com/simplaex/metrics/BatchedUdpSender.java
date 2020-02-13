package com.simplaex.metrics;

import lombok.Builder;
import lombok.Setter;
import lombok.Value;
import lombok.extern.log4j.Log4j2;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.PortUnreachableException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Log4j2
public class BatchedUdpSender {

  public static class CreationException extends RuntimeException {
    private CreationException(final Throwable cause) {
      super(cause);
    }
  }

  public static final int MAXIMUM_UDP_PAYLOAD_SIZE = 65507;

  private final DatagramChannel channel;
  private final Selector selector;
  private final ByteBuffer buffer;
  private final ScheduledExecutorService loop;

  @Setter
  private int batchThreshold = 0;

  private boolean overflowing = false;

  @Nonnull
  private static DatagramChannel openChannel(
    @Nonnull final String host,
    @Nonnegative final int port,
    @Nonnull final Selector selector) throws IOException {
    final InetSocketAddress address = new InetSocketAddress(host, port);
    final DatagramChannel channel = DatagramChannel.open();
    channel.configureBlocking(false);
    channel.connect(address);
    channel.register(selector, SelectionKey.OP_WRITE);
    return channel;
  }

  @Value
  @Builder
  public static class Options {
    final Duration flushInterval;
    final int batchThreshold;
  }

  public static Options DEFAULT_OPTIONS = new Options(Duration.ofMillis(100), 0);

  @Nonnull
  public static Options.OptionsBuilder options() {
    return Options.builder();
  }

  public BatchedUdpSender() {
    this("localhost", 8125);
  }

  public BatchedUdpSender(
    @Nonnull final String host,
    @Nonnegative final int port
  ) {
    this(host, port, null);
  }

  public BatchedUdpSender(
    @Nonnull final String host,
    @Nonnegative final int port,
    @Nullable final Options options
  ) {
    try {
      this.buffer = ByteBuffer.allocateDirect(MAXIMUM_UDP_PAYLOAD_SIZE);
      this.loop = Executors.newScheduledThreadPool(1, runnable -> {
        final Thread thread = Executors.defaultThreadFactory().newThread(runnable);
        thread.setName(this.getClass().getName());
        thread.setDaemon(true);
        return thread;
      });
      this.selector = Selector.open();
      this.channel = openChannel(host, port, selector);
      final Options opts = Optional.ofNullable(options).orElse(DEFAULT_OPTIONS);
      final long flushIntervalMs = opts.getFlushInterval().toMillis();
      setBatchThreshold(opts.getBatchThreshold());
      loop.scheduleAtFixedRate(this::sendBuffer, flushIntervalMs, flushIntervalMs, TimeUnit.MILLISECONDS);
    } catch (final Exception exc) {
      throw new CreationException(exc);
    }
  }

  private void execute(@Nonnull final Runnable action) {
    loop.execute(() -> {
      if (overflowing) {
        return;
      }
      action.run();
    });
  }

  public void send(@Nonnull final byte[] bytes, @Nonnegative final int from, @Nonnegative final int to) {
    execute(() -> sendInternal(bytes, from, to));
  }

  public void send(@Nonnull final byte[] bytes) {
    execute(() -> sendInternal(bytes));
  }

  public void send(@Nonnull final String string) {
    execute(() -> sendInternal(string.getBytes(StandardCharsets.UTF_8)));
  }

  private void sendBuffer() {
    try {
      if (buffer.remaining() == buffer.capacity()) {
        return;
      }
      selector.selectNow();
      for (final SelectionKey key : selector.selectedKeys()) {
        if (key.isWritable()) {
          try {
            buffer.flip();
            channel.write(buffer);
          } catch (final PortUnreachableException ignore) {
            // other end is simply not available, that's okay, it's UDP baby
          } finally {
            buffer.clear();
            overflowing = false;
          }
          return;
        }
      }
    } catch (final IOException exc) {
      log.error("Could not send udp data", exc);
    } catch (final Exception exc) {
      log.error("Some unexpected exception happened while trying to send UDP data", exc);
    }
  }

  private void sendInternal(final byte[] bytes) {
    sendInternal(bytes, 0, bytes.length);
  }

  private void sendInternal(final byte[] bytes, final int from, final int to) {
    try {
      final int len = to - from;
      if (len > buffer.capacity()) {
        log.error("Trying to send something which can not possibly fit into a UDP package");
        return;
      }
      if (len > buffer.remaining()) {
        sendBuffer();
      }
      buffer.put(bytes, from, to);
      if (buffer.position() > batchThreshold) {
        sendBuffer();
      }
    } catch (final BufferOverflowException ignore) {
      overflowing = true;
      loop.execute(this::sendBuffer);
      log.warn("New events are being produced too fast and could not be added to the buffer");
    }
  }
}
