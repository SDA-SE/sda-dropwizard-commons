package com.sdase.commons.server.consumer.filter;

import com.sdase.commons.shared.tracing.ConsumerTracing;
import org.slf4j.MDC;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;

/**
 * A request filter that detects, verifies and provides the consumer token in incoming requests.
 */
public class ConsumerTokenServerFilter implements ContainerRequestFilter {

   private boolean requireIdentifiedConsumer;

   /**
    * @param requireIdentifiedConsumer if an identified customer is required to fulfill requests
    */
   public ConsumerTokenServerFilter(boolean requireIdentifiedConsumer) {
      this.requireIdentifiedConsumer = requireIdentifiedConsumer;
   }

   @Override
   public void filter(ContainerRequestContext requestContext) {

      // In case of OPTIONS, no headers can be provided. Usually OPTION requests are from browsers for CORS.
      if (HttpMethod.OPTIONS.equals(requestContext.getMethod())) {
         return;
      }

      Optional<String> consumerToken = extractConsumerTokenFromRequest(requestContext);
      Optional<String> consumerName = consumerToken.map(this::extractConsumerName);

      consumerToken.ifPresent(token -> this.addConsumerTokenToRequest(requestContext, token));
      consumerName.ifPresent(this::addConsumerNameToMdc);
      consumerName.ifPresent(name -> this.addConsumerNameToRequest(requestContext, name));

      if (requireIdentifiedConsumer && !consumerName.isPresent()) {
         throw failForMissingConsumerToken();
      }

   }

   private Optional<String> extractConsumerTokenFromRequest(ContainerRequestContext requestContext) {
      String consumerToken = requestContext.getHeaderString(ConsumerTracing.TOKEN_HEADER);
      if (consumerToken == null || consumerToken.trim().isEmpty()) {
         return Optional.empty();
      }
      return Optional.of(consumerToken);
   }

   private String extractConsumerName(String consumerToken) {
      // TODO: Verify and parse token (for now the token is the consumer name)
      return consumerToken;
   }

   private void addConsumerTokenToRequest(ContainerRequestContext requestContext, String consumerToken) {
      requestContext.setProperty(ConsumerTracing.TOKEN_ATTRIBUTE, consumerToken);
   }

   private void addConsumerNameToMdc(String consumerName) {
      if (MDC.getMDCAdapter() != null) {
         MDC.put(ConsumerTracing.NAME_MDC_KEY, consumerName);
      }
   }

   private void addConsumerNameToRequest(ContainerRequestContext requestContext, String consumerName) {
      requestContext.setProperty(ConsumerTracing.NAME_ATTRIBUTE, consumerName);
   }

   private RuntimeException failForMissingConsumerToken() {
      // TODO response should be json with errors as defined in the API guide (after that guide is implemented for 422)
      Response response = Response
            .status(Response.Status.UNAUTHORIZED)
            .type(MediaType.TEXT_PLAIN_TYPE)
            .entity("Consumer token is required to access this resource.")
            .build();
      return new WebApplicationException(response);
   }
}
