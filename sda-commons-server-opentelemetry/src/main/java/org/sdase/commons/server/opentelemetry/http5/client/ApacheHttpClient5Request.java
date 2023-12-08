package org.sdase.commons.server.opentelemetry.http5.client;

import static java.util.logging.Level.FINE;

import jakarta.annotation.Nullable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.ProtocolVersion;

public final class ApacheHttpClient5Request {

  private static final Logger logger = Logger.getLogger(ApacheHttpClient5Request.class.getName());

  @Nullable private final URI uri;

  private final HttpRequest delegate;
  @Nullable private final HttpHost target;

  ApacheHttpClient5Request(@Nullable HttpHost httpHost, HttpRequest httpRequest) {
    URI calculatedUri = getUri(httpRequest);
    if (calculatedUri != null && httpHost != null) {
      uri = getCalculatedUri(httpHost, calculatedUri);
    } else {
      uri = calculatedUri;
    }
    delegate = httpRequest;
    target = httpHost;
  }

  List<String> getHeader(String name) {
    return headersToList(delegate.getHeaders(name));
  }

  // minimize memory overhead by not using streams
  static List<String> headersToList(Header[] headers) {
    if (headers.length == 0) {
      return Collections.emptyList();
    }
    List<String> headersList = new ArrayList<>(headers.length);
    for (Header header : headers) {
      headersList.add(header.getValue());
    }
    return headersList;
  }

  String getMethod() {
    return delegate.getMethod();
  }

  @Nullable
  String getUrl() {
    return uri != null ? uri.toString() : null;
  }

  String getProtocolName() {
    return delegate.getVersion().getProtocol();
  }

  String getProtocolVersion() {
    ProtocolVersion protocolVersion = delegate.getVersion();
    if (protocolVersion.getMinor() == 0) {
      return Integer.toString(protocolVersion.getMajor());
    }
    return protocolVersion.getMajor() + "." + protocolVersion.getMinor();
  }

  @Nullable
  public String getServerAddress() {
    return uri == null ? null : uri.getHost();
  }

  @Nullable
  public Integer getServerPort() {
    return uri == null ? null : uri.getPort();
  }

  @Nullable
  private static URI getUri(HttpRequest httpRequest) {
    try {
      // this can be relative or absolute
      return new URI(httpRequest.getUri().toString());
    } catch (URISyntaxException e) {
      logger.log(FINE, e.getMessage(), e);
      return null;
    }
  }

  @Nullable
  private static URI getCalculatedUri(HttpHost httpHost, URI uri) {
    try {
      return new URI(
          httpHost.getSchemeName(),
          uri.getUserInfo(),
          httpHost.getHostName(),
          httpHost.getPort(),
          uri.getPath(),
          uri.getQuery(),
          uri.getFragment());
    } catch (URISyntaxException e) {
      logger.log(FINE, e.getMessage(), e);
      return null;
    }
  }

  @Nullable
  public InetSocketAddress getServerSocketAddress() {
    if (target == null) {
      return null;
    }
    InetAddress inetAddress = target.getAddress();
    return inetAddress == null ? null : new InetSocketAddress(inetAddress, target.getPort());
  }
}
