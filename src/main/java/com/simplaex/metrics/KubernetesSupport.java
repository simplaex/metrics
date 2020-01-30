package com.simplaex.metrics;

import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;

import javax.annotation.Nonnull;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility to retrieve the host ip of the node the pod is running on from the kubernetes API.
 * <p>
 * See {@link #getHostIp()}.
 *
 * @author Julian Fleischer
 */
@SuppressWarnings("FieldCanBeLocal")
@UtilityClass
@Log4j2
public class KubernetesSupport {

  private static String KUBERNETES_DEFAULT_APISERVER = "kubernetes.default.svc";
  private static int KUBERNETES_DEFAULT_PORT = 443;
  private static String KUBERNETES_SERVICEACCOUNT = "/var/run/secrets/kubernetes.io/serviceaccount";

  /**
   * Get the namespace of the pod from the kubernetes serviceaccount secret.
   */
  public static String getKubernetesNamespace() {
    try {
      final byte[] bytes = Files.readAllBytes(Paths.get(KUBERNETES_SERVICEACCOUNT, "namespace"));
      return new String(bytes, StandardCharsets.UTF_8);
    } catch (final IOException exc) {
      throw new RuntimeException(exc);
    }
  }

  /**
   * Get the token for the pods serviceaccount to communicate with the kubernetes api.
   */
  @Nonnull
  public static String getKubernetesToken() {
    try {
      final byte[] bytes = Files.readAllBytes(Paths.get(KUBERNETES_SERVICEACCOUNT, "token"));
      return new String(bytes, StandardCharsets.UTF_8);
    } catch (final IOException exc) {
      throw new RuntimeException(exc);
    }
  }

  /**
   * Get the hostname of the kuberentes api server.
   */
  @Nonnull
  public static String getKubernetesHost() {
    final String host = System.getenv("KUBERNETES_SERVICE_HOST");
    if (host == null) {
      return KUBERNETES_DEFAULT_APISERVER;
    }
    return host;
  }

  public static int getKubernetesPort() {
    try {
      return Integer.parseInt(System.getenv("KUBERNETES_PORT_443_TCP_PORT"));
    } catch (final Exception exc) {
      return KUBERNETES_DEFAULT_PORT;
    }
  }

  /**
   * Get the name of this pod (which is its hostname).
   */
  public static String getHostname() {
    return System.getenv("HOSTNAME");
  }

  /**
   * Get the host ip of the node this pod is running on form the kubernetes api server.
   */
  public static String getHostIp() {
    try {
      final String kubeHost = getKubernetesHost();
      final int kubePort = getKubernetesPort();
      final String namespace = getKubernetesNamespace();
      final String hostname = getHostname();
      final String token = getKubernetesToken();
      final String uri = String.format(
        "https://%s:%s/api/v1/namespaces/%s/pods/%s", kubeHost, kubePort, namespace, hostname);
      final URLConnection conn = new URL(uri).openConnection();
      if (conn instanceof HttpsURLConnection) {
        final SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, new TrustManager[]{
          new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
              return null;
            }

            public void checkClientTrusted(final X509Certificate[] certs, final String authType) {
            }

            public void checkServerTrusted(final X509Certificate[] certs, final String authType) {
            }
          }
        }, new SecureRandom());
        ((HttpsURLConnection) conn).setSSLSocketFactory(sc.getSocketFactory());
      }
      conn.addRequestProperty("Authorization", "Bearer " + token);
      final InputStream s = conn.getInputStream();
      final String r = new Scanner(s).useDelimiter("\\A").next();
      final Matcher m = Pattern.compile("\"hostIP\"[^,]+\"([^\"]+)\"").matcher(r);
      if (!m.find()) {
        return "";
      }
      return m.group(1);
    } catch (final Exception exc) {
      throw new RuntimeException(exc);
    }
  }

}
