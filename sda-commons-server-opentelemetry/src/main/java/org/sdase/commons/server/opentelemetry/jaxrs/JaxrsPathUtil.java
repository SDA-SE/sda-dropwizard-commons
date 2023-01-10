package org.sdase.commons.server.opentelemetry.jaxrs;

public final class JaxrsPathUtil {
  private static final String SEPARATOR = "/";

  private JaxrsPathUtil() {}

  public static String normalizePath(String path) {
    // ensure that non-empty path starts with /
    if (path == null || SEPARATOR.equals(path)) {
      path = "";
    } else if (!path.startsWith(SEPARATOR)) {
      path = SEPARATOR + path;
    }
    // remove trailing /
    if (path.endsWith(SEPARATOR)) {
      path = path.substring(0, path.length() - 1);
    }

    return path;
  }
}
