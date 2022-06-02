package org.sdase.commons.server.security.filter;

import static java.util.Arrays.asList;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;

/**
 * This filter adds headers to the response that enhance the security of web applications. Usually
 * we do not provide web content from services. But we address the risks identified in the security
 * guide as:
 *
 * <ul>
 *   <li>"Risiko: Clickjacking"
 *   <li>"Risiko: Interpretation von Inhalten durch den Browser"
 *   <li>"Risiko: Cross Site Scripting (XSS)"
 *   <li>"Risiko: Weitergabe von besuchten URLs an Dritte"
 *   <li>"Risiko: Nachladen von Inhalten in Flash und PDFs"
 * </ul>
 */
public class WebSecurityApiOnlyHeaderFilter implements ContainerResponseFilter {

  private static final Map<String, String> WEB_SECURITY_HEADERS = initWebSecurityHeaders();

  private static Map<String, String> initWebSecurityHeaders() {
    Map<String, String> webSecurityHeaders = new LinkedHashMap<>();
    webSecurityHeaders.put("X-Frame-Options", "DENY");
    webSecurityHeaders.put("X-Content-Type-Options", "nosniff");
    webSecurityHeaders.put("X-XSS-Protection", "1; mode=block");
    webSecurityHeaders.put("Referrer-Policy", "same-origin");
    webSecurityHeaders.put("X-Permitted-Cross-Domain-Policies", "none");
    webSecurityHeaders.put(
        "Content-Security-Policy",
        String.join("; ", asList("default-src 'none'", "frame-ancestors 'none'", "sandbox")));
    return webSecurityHeaders;
  }

  @Override
  public void filter(
      ContainerRequestContext requestContext, ContainerResponseContext responseContext)
      throws IOException {
    WEB_SECURITY_HEADERS.forEach((name, value) -> addHeaderIfAbsent(responseContext, name, value));
  }

  private void addHeaderIfAbsent(
      ContainerResponseContext response, String headerName, String headerValue) {
    if (response.getHeaders().get(headerName) == null
        || response.getHeaders().get(headerName).isEmpty()) {
      response.getHeaders().add(headerName, headerValue);
    }
  }
}
