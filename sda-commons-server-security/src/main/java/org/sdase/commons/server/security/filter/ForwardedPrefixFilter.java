package org.sdase.commons.server.security.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * This filter is used to apply the X-Forwarded-Prefix header and adds it as a prefix to the
 * requested URI. This leverages the behavior of Jetty's
 * {@link org.eclipse.jetty.server.ForwardedRequestCustomizer which does not support prefix when
 * handling forwarded requests.
 */
public class ForwardedPrefixFilter implements Filter {

  @Override
  public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
      throws IOException, ServletException {

    HttpServletRequest request = (HttpServletRequest) req;
    HttpServletResponse response = (HttpServletResponse) res;

    String prefix = request.getHeader("X-Forwarded-Prefix");

    if (prefix != null && !prefix.isEmpty()) {
      chain.doFilter(
          new HttpServletRequestWrapper(request) {
            @Override
            public String getRequestURI() {
              return prefix + super.getRequestURI();
            }

            @Override
            public String getContextPath() {
              return prefix;
            }
          },
          response);
    } else {
      chain.doFilter(request, response);
    }
  }
}
