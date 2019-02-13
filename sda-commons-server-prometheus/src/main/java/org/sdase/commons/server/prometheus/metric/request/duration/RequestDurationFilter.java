package org.sdase.commons.server.prometheus.metric.request.duration;

import io.prometheus.client.SimpleTimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.ResourceInfo;

@Priority(Priorities.AUTHENTICATION - 50) // Before authentication to track unauthenticated request
// and before consumer-token filter to track requests without consumer tokens. We can still log the
// consumer because we access it while responding.
public class RequestDurationFilter implements ContainerRequestFilter, ContainerResponseFilter {

   private static final Logger LOG = LoggerFactory.getLogger(RequestDurationFilter.class);
   private static final String TIMER_REQUEST_PROPERTY = RequestDurationFilter.class.getName() + ".timer";

   private final ResourceInfo resourceInfo;
   private final RequestDurationHistogramSpecification requestDurationHistogramSpecification;

   public RequestDurationFilter(ResourceInfo resourceInfo, RequestDurationHistogramSpecification requestDurationHistogramSpecification) {
      this.resourceInfo = resourceInfo;
      this.requestDurationHistogramSpecification = requestDurationHistogramSpecification;
   }

   @Override
   public void filter(ContainerRequestContext requestContext) {
      LOG.trace("starting request {}", requestContext.getMethod());
      // starting the timer
      requestContext.setProperty(TIMER_REQUEST_PROPERTY, new SimpleTimer());
   }

   @Override
   public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {

      Object timerObject = requestContext.getProperty(TIMER_REQUEST_PROPERTY);
      if (!validateTimerObject(timerObject)) {
         return;
      }
      SimpleTimer timer = (SimpleTimer) timerObject;
      // stopping the timer
      double elapsedSeconds = timer.elapsedSeconds();
      requestDurationHistogramSpecification.observe(elapsedSeconds, resourceInfo, requestContext, responseContext);

      LOG.trace("calculated duration {} for request {}", elapsedSeconds, requestContext.getMethod());
   }

   private boolean validateTimerObject(Object timerObject) {
      if (timerObject == null) {
         return false;
      }
      if (!(timerObject instanceof SimpleTimer)) {
         LOG.warn("Expecting {} in request context to be of type {} but found property value of type {}",
               TIMER_REQUEST_PROPERTY, SimpleTimer.class, timerObject.getClass());
         return false;
      }
      return true;
   }
}
