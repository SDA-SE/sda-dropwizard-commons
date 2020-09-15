package org.sda.commons.server.jackson.hal;

import io.openapitools.jackson.dataformat.hal.HALLink;
import io.openapitools.jackson.dataformat.hal.HALLink.Builder;
import java.net.URI;

/** Wrapper class to provide the processed Link as {@linkplain URI} or {@linkplain HALLink} */
public final class LinkResult {
  private final URI uri;
  private final HALLink halLink;

  /**
   * Instantiates a new Link result.
   *
   * @param uri the uri
   */
  public LinkResult(URI uri) {
    this.uri = uri;
    this.halLink = new Builder(uri).build();
  }

  /**
   * Returns the link result as {@linkplain URI}
   *
   * @return the uri
   */
  public URI asUri() {
    return this.uri;
  }

  /**
   * Returns the link result as {@linkplain HALLink}
   *
   * @return the hal link
   */
  public HALLink asHalLink() {
    return halLink;
  }
}
