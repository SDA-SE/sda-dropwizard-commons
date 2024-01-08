package org.sdase.commons.server.weld.testing.test.servlets;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.sdase.commons.server.weld.testing.test.util.BarSupplier;

public class TestServlet extends HttpServlet {

  @Inject private BarSupplier bar;

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    response.setContentType("text/html");
    response.setStatus(HttpServletResponse.SC_OK);
    response.getWriter().println("<h1>Hello from MyServlet</h1>" + bar.get());
  }
}
