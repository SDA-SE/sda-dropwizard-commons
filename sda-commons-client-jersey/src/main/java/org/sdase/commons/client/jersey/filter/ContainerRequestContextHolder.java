package org.sdase.commons.client.jersey.filter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import java.util.Optional;

/**
 * A filter that provides the current request from a {@link ThreadLocal}. Currently it is only implemented for the
 * client filters in this package.
 */
public class ContainerRequestContextHolder implements ContainerRequestFilter, ContainerResponseFilter {

   private static final ThreadLocal<ContainerRequestContext> REQUEST_CONTEXT_HOLDER = new ThreadLocal<>();

   /**
    * @return The current {@link ContainerRequestContext} if the current {@link Thread} is in a request context. An
    *         empty {@code Optional} if the current thread is not in a request context.
    */
   static Optional<ContainerRequestContext> currentRequestContext() {
      return Optional.ofNullable(REQUEST_CONTEXT_HOLDER.get());
   }

   @Override
   public void filter(ContainerRequestContext requestContext) {
      REQUEST_CONTEXT_HOLDER.set(requestContext);
   }

   @Override
   public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
      REQUEST_CONTEXT_HOLDER.remove();
   }
}
