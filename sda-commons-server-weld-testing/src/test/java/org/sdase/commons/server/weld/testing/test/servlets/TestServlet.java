package org.sdase.commons.server.weld.testing.test.servlets;

import org.sdase.commons.server.weld.testing.test.util.BarSupplier;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class TestServlet extends HttpServlet {

   private static final long serialVersionUID = -8600530309252834103L;

   @Inject
   private BarSupplier bar;

   @Override
   protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
      response.setContentType("text/html");
      response.setStatus(HttpServletResponse.SC_OK);
      response.getWriter().println("<h1>Hello from MyServlet</h1>" + bar.get());
   }
}
