package org.sda.commons.server.jackson.hal;

import io.openapitools.jackson.dataformat.hal.HALLink;
import java.net.URI;

/**
 * Wrapper class to provide the processed Link as {@linkplain URI} or {@linkplain HALLink}
 *
 * @deprecated this package has been created by mistake. The {@code LinkResult} moved to {@link
 *     org.sdase.commons.server.jackson.hal.LinkResult}, please update the imports.
 */
@Deprecated
public final class LinkResult extends org.sdase.commons.server.jackson.hal.LinkResult {

  /**
   * Instantiates a new Link result.
   *
   * @param uri the uri
   */
  public LinkResult(URI uri) {
    super(uri);
  }
}
