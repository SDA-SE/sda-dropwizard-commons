package org.sda.commons.server.jackson.hal;

/**
 * @deprecated this package has been created by mistake. The {@code
 *     HalLinkMethodInvocationException} moved to {@link
 *     org.sdase.commons.server.jackson.hal.HalLinkMethodInvocationException}, please update the
 *     imports.
 */
@Deprecated
public class HalLinkMethodInvocationException
    extends org.sdase.commons.server.jackson.hal.HalLinkMethodInvocationException {

  public HalLinkMethodInvocationException(String message) {
    super(message);
  }

  public HalLinkMethodInvocationException(String message, Throwable cause) {
    super(message, cause);
  }
}
