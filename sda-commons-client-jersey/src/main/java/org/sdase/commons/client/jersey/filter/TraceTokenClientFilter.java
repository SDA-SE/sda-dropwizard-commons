package org.sdase.commons.client.jersey.filter;

import org.sdase.commons.shared.tracing.RequestTracing;
import org.slf4j.MDC;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import java.util.Optional;
import java.util.UUID;

import static org.sdase.commons.client.jersey.filter.ContainerRequestContextHolder.currentRequestContext;

/**
 * The  @{@link TraceTokenClientFilter} adds a trace token to client requests. If existing, the trace token
 * is retrieved from the incoming request. If not existing, a new token is generated and added to MDC and
 * to the request as header.
 */
public class TraceTokenClientFilter implements ClientRequestFilter, ClientResponseFilter {

   private static final String ADDED_TO_MDC_FOR_REQUEST = TraceTokenClientFilter.class.getName() + "_ADDED_TO_MDC";

   @Override
   public void filter(ClientRequestContext requestContext) {
      String traceToken = takeTraceTokenFromIncomingRequest()
            .orElseGet(() -> createTraceTokenForOutgoingRequest(requestContext));
      requestContext.getHeaders().add(RequestTracing.TOKEN_HEADER, traceToken);
   }

   @Override
   public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) {
      if (Boolean.TRUE.equals(requestContext.getProperty(ADDED_TO_MDC_FOR_REQUEST))) {
         requestContext.removeProperty(ADDED_TO_MDC_FOR_REQUEST);
         MDC.getMDCAdapter().remove(RequestTracing.TOKEN_MDC_KEY);
      }
   }

   private String createTraceTokenForOutgoingRequest(ClientRequestContext clientRequestContext) {
      String traceToken = UUID.randomUUID().toString();
      addNewTokenToMdcForCurrentRequest(clientRequestContext, traceToken);
      return traceToken;
   }

   private void addNewTokenToMdcForCurrentRequest(ClientRequestContext requestContext, String newTraceToken) {
      if (MDC.getMDCAdapter() != null) {
         MDC.put(RequestTracing.TOKEN_MDC_KEY, newTraceToken);
         requestContext.setProperty(ADDED_TO_MDC_FOR_REQUEST, Boolean.TRUE);
      }
   }

   private Optional<String> takeTraceTokenFromIncomingRequest() {
      return currentRequestContext()
            .map(crc -> crc.getProperty(RequestTracing.TOKEN_ATTRIBUTE))
            .map(Object::toString);
   }
}
