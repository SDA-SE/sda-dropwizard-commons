package org.sdase.commons.server.healthcheck.servlet;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckFilter;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.json.HealthCheckModule;
import com.codahale.metrics.servlets.HealthCheckServlet;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.ExecutorService;

/**
 * Servlet that provides only the <b>internal</b> health check data of the application as JSON response
 */
public class OnlyInternalHealthCheckServlet extends HttpServlet {

   private static final String HEALTH_CHECK_EXECUTOR = HealthCheckServlet.class.getCanonicalName() + ".executor";

   private final transient HealthCheckRegistry healthCheckRegistry;
   private transient ExecutorService executorService;
   private transient HealthCheckFilter healthCheckFilter;
   private final transient ObjectMapper mapper;

   public OnlyInternalHealthCheckServlet(HealthCheckRegistry healthCheckRegistry) {
      this.healthCheckRegistry = healthCheckRegistry;
      this.mapper = new ObjectMapper().registerModule(new HealthCheckModule());
   }

   @Override
   public void init(ServletConfig config) throws ServletException {
      super.init(config);

      final ServletContext context = config.getServletContext();
      final Object executorAttr = context.getAttribute(HEALTH_CHECK_EXECUTOR);

      if (executorAttr instanceof ExecutorService) {
         executorService = (ExecutorService) executorAttr;
      }

      healthCheckFilter = new OnlyInternalHealthCheckFilter();
   }

   @Override
   public void destroy() {
      super.destroy();
      healthCheckRegistry.shutdown();
   }

   @Override
   protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
      final Map<String, HealthCheck.Result> results = runHealthChecks();

      resp.setContentType(MediaType.APPLICATION_JSON);
      resp.setHeader("Cache-Control", "must-revalidate,no-cache,no-store");

      if (results.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);

      } else {
         if (isAllHealthy(results)) {
            resp.setStatus(HttpServletResponse.SC_OK);
         } else {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
         }

         try {
            getWriter(Boolean.parseBoolean(req.getParameter("pretty"))).writeValue(resp.getWriter(), results);
         } catch (IOException e) {
            // nothing to do here, sonar likes to have this exception caught: squid:S1989
         }
      }
   }

   private ObjectWriter getWriter(boolean prettyPrint) {
      return prettyPrint ? mapper.writerWithDefaultPrettyPrinter() : mapper.writer();
   }

   private SortedMap<String, HealthCheck.Result> runHealthChecks() {
      if (executorService == null) {
         return healthCheckRegistry.runHealthChecks(healthCheckFilter);
      }
      return healthCheckRegistry.runHealthChecks(executorService, healthCheckFilter);
   }

   private static boolean isAllHealthy(Map<String, HealthCheck.Result> results) {
      return results.values().stream().allMatch(HealthCheck.Result::isHealthy);
   }
}