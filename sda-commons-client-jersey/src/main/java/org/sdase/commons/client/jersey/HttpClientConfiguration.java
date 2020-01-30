package org.sdase.commons.client.jersey;

public class HttpClientConfiguration {
  // Chunked encoding is disabled by default, because in combination with the
  // underlying Apache Http Client it breaks support for multipart/form-data
  private boolean chunkedEncodingEnabled = false;
  private boolean gzipEnabled = true;
  // Gzip for request is disabled by default, because it breaks with some
  // server implementations in combination with the Content-Encoding: gzip
  // header if supplied accidentally to a GET request. This happen after
  // calling a POST and receiving a SEE_OTHER response that is executed with
  // the same headers as the POST request.
  private boolean gzipEnabledForRequests = false;

  /**
   * Returns if the http client should use chunked encoding, disabled by default.
   *
   * @return
   */
  public boolean isChunkedEncodingEnabled() {
    return chunkedEncodingEnabled;
  }

  /**
   * Configures if the http client should use chunked encoding.
   *
   * @param chunkedEncodingEnabled
   * @return
   */
  public HttpClientConfiguration setChunkedEncodingEnabled(boolean chunkedEncodingEnabled) {
    this.chunkedEncodingEnabled = chunkedEncodingEnabled;
    return this;
  }

  /**
   * Returns if the http client should accept gzipped responses, enabled by default.
   *
   * @return
   */
  public boolean isGzipEnabled() {
    return gzipEnabled;
  }

  /**
   * Configure if the http client should accept gzipped responses.
   *
   * @param gzipEnabled
   * @return
   */
  public HttpClientConfiguration setGzipEnabled(boolean gzipEnabled) {
    this.gzipEnabled = gzipEnabled;
    return this;
  }

  /**
   * Returns if the http client should gzip request bodies, disabled by default.
   *
   * @return
   */
  public boolean isGzipEnabledForRequests() {
    return gzipEnabledForRequests;
  }

  /**
   * Configure if the http client should gzip request bodies.
   *
   * @param gzipEnabledForRequests
   * @return
   */
  public HttpClientConfiguration setGzipEnabledForRequests(boolean gzipEnabledForRequests) {
    this.gzipEnabledForRequests = gzipEnabledForRequests;
    return this;
  }
}
