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
import java.util.Optional;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility to retrieve the host ip of the node the pod is running on from the kubernetes API.
 * <p>
 * See {@link #getHostIp()}.
 * <p>
 * The following environment variables affect the behavior of this utility:
 * <dl>
 *   <dt><code>KUBERNETES_SERVICE_HOST</code></dt>
 *   <dd>Defaults to <code>kubernetes.default.svc</code></dd>
 *
 *   <dt><code>KUBERNETES_SERVICEACCOUNT_SECRETS_PATH</code></dt>
 *   <dd>Defaults to <code>/var/run/secrets/kubernetes.io/serviceaccount</code></dd>
 *
 *   <dt><code>KUBERNETES_SERVICEACCOUNT_TOKEN</code></dt>
 *   <dd>Defaults to the value read from <code>KUBERNETES_SERVICE_ACCOUNT_SECRETS_PATH + "/token"</code></dd>
 *
 *   <dt><code>KUBERNETES_PORT_443_TCP_PORT</code></dt>
 *   <dd>Defaults to 443</dd>
 *
 *    <dt><code>HOSTNAME</code></dt>
 *    <dd>The name of the pod/host. If this environment variable is not set it will attempt to call
 *    the <code>hostname</code> cli tool.</dd>
 *
 *    <dt><code>KUBERNETES_HOSTIP_REGEX</code></dt>
 *    <dd>The regular expression which extracts the hostIP form the reponse of the API server.
 *    Defaults to <code>"hostIP"[^,"]+"([^"]+)"</code></dd>
 * </dl>
 *
 * @author Julian Fleischer
 */
@SuppressWarnings("FieldCanBeLocal")
@UtilityClass
@Log4j2
public class KubernetesSupport {

  private static String KUBERNETES_SERVICE_HOST = Optional
    .ofNullable(System.getenv("KUBERNETES_SERVICE_HOST"))
    .orElse("kubernetes.default.svc");
  private static int KUBERNETES_DEFAULT_PORT = 443;
  private static String KUBERNETES_SERVICEACCOUNT = Optional
    .ofNullable(System.getenv("KUBERNETES_SERVICEACCOUNT_SECRETS_PATH"))
    .orElse("/var/run/secrets/kubernetes.io/serviceaccount");

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
    final String token = System.getenv("KUBERNETES_SERVICEACCOUNT_TOKEN");
    if (token != null) {
      return token;
    }
    try {
      final byte[] bytes = Files.readAllBytes(Paths.get(KUBERNETES_SERVICEACCOUNT, "token"));
      return new String(bytes, StandardCharsets.UTF_8);
    } catch (final IOException exc) {
      throw new RuntimeException(exc);
    }
  }

  public static int getKubernetesServicePort() {
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
    final String hostname = System.getenv("HOSTNAME");
    if (hostname != null && !hostname.isEmpty()) {
      log.info("Detected hostname via HOSTNAME environment variable ({})", hostname);
      return hostname;
    }
    try {
      final Process hostnamePs = Runtime.getRuntime().exec("hostname");
      hostnamePs.waitFor();
      final String hostnameFromPs = new java.util.Scanner(hostnamePs.getInputStream()).useDelimiter("\\A").next();
      log.info("Determined hostname via `hostname` command ({})", hostname);
      return hostnameFromPs;
    } catch (final Exception exc) {
      throw new RuntimeException(exc);
    }
  }

  /**
   * Gets the instance IP if you're running in EKS/AWS.
   * <p>
   * See https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-instance-metadata.html
   */
  public static String getInstanceIp() throws Exception {
    final String url = Optional.ofNullable(System.getenv("INSTANCE_METADATA_LOCAL_IPV4_URL"))
      .orElse("http://169.254.169.254/latest/meta-data/local-ipv4");
    return new Scanner(new URL(url).openConnection().getInputStream()).next("\\A");
  }

  /**
   * Get the host ip of the node this pod is running on form the kubernetes api server.
   * <p>
   * For this to work the serviceaccount of the pod needs to be able to get it's own
   * resource from the API server.
   */
  public static String getHostIp() {
    try {
      return getInstanceIp();
    } catch (final Exception ignore) {
      // ignore and try to get directly from kubernetes instead
    }
    try {
      final String kubeHost = KUBERNETES_SERVICE_HOST;
      final int kubePort = getKubernetesServicePort();
      final String namespace = getKubernetesNamespace();
      final String hostname = getHostname();
      final String token = getKubernetesToken();
      final String uri = String.format(
        "https://%s:%s/api/v1/namespaces/%s/pods/%s", kubeHost, kubePort, namespace, hostname);
      log.info("Querying pod details from uri={} using token={}", uri, token);
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
      final InputStream in = conn.getInputStream();
      final String apiResponse = new Scanner(in).useDelimiter("\\A").next();
      final String regex = Optional
        .ofNullable(System.getenv("KUBERNETES_HOSTIP_REGEX"))
        .orElse("\"hostIP\"[^,\"]+\"([^\"]+)\"");
      final Matcher m = Pattern.compile(regex).matcher(apiResponse);
      log.debug("Response from Kubernetes API: {}", apiResponse);
      if (!m.find()) {
        log.error("Could not find regex={} (can be set via KUBERNETES_HOSTIP_REGEX) in API response", regex);
        return "";
      }
      return m.group(1);
    } catch (final Exception exc) {
      throw new RuntimeException(exc);
    }
  }

}
