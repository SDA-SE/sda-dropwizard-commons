package org.sdase.commons.client.jersey.error;

import com.fasterxml.jackson.core.type.TypeReference;
import org.sdase.commons.shared.api.error.ApiError;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * A utility class to support handling of errors that are received from an outgoing request.
 */
public class ClientErrorUtil {

   private ClientErrorUtil() {
   }

   /**
    * Extracts the standard platform error object from the failed response.
    *
    * @param response the response that contains an error
    * @return the error data or {@code null} if the response is neither a client error (4xx) nor a server error (5xx).
    * @throws ProcessingException   if the content of the message cannot be
    *                               mapped to an entity of the requested type. See {@link Response#readEntity(Class)}
    * @throws IllegalStateException if the entity is not backed by an input stream,
    *                               the response has been {@link Response#close() closed} already,
    *                               or if the entity input stream has been fully consumed already and has
    *                               not been buffered prior consuming. See {@link Response#readEntity(Class)}
    */
   public static ApiError readErrorBody(Response response) {
      return readErrorBody(response, ApiError.class);
   }

   /**
    * Extracts the standard platform error object from the client exception.
    *
    * @param e the error occurred when requesting a resource.
    * @return the error data  or {@code null} if the given {@link ClientRequestException} does not contain a
    *         {@link WebApplicationException} with a {@link Response} that is a client error (4xx) or a server error
    *         (5xx).
    * @throws ProcessingException   if the content of the message cannot be
    *                               mapped to an entity of the requested type. See {@link Response#readEntity(Class)}
    * @throws IllegalStateException if the entity is not backed by an input stream,
    *                               the response has been {@link Response#close() closed} already,
    *                               or if the entity input stream has been fully consumed already and has
    *                               not been buffered prior consuming. See {@link Response#readEntity(Class)}
    */
   public static ApiError readErrorBody(ClientRequestException e) {
      return readErrorBody(e, ApiError.class);
   }

   /**
    * Read json body of the given {@code response} as {@code errorType}.
    *
    * @param response the response that produced an error.
    * @param errorType the expected type of the data in the response body
    * @param <E> the type the response body is converted to
    * @return the response body converted to the desired {@code errorType} or {@code null} if the response is neither a
    *         client error (4xx) nor a server error (5xx).
    * @throws ProcessingException   if the content of the message cannot be
    *                               mapped to an entity of the requested type. See {@link Response#readEntity(Class)}
    * @throws IllegalStateException if the entity is not backed by an input stream,
    *                               the response has been {@link Response#close() closed} already,
    *                               or if the entity input stream has been fully consumed already and has
    *                               not been buffered prior consuming. See {@link Response#readEntity(Class)}
    */
   public static <E> E readErrorBody(Response response, Class<E> errorType) {
      if (response.getStatus() > 399) {
         return response.readEntity(errorType);
      } else {
         return null;
      }
   }

   /**
    * Read json body of the given {@code response} as {@code errorType}.
    *
    * @param response the response that produced an error.
    * @param errorType the expected type of the data in the response body
    * @param <E> the type the response body is converted to
    * @return the response body converted to the desired {@code errorType} or {@code null} if the response is neither a
    *         client error (4xx) nor a server error (5xx).
    * @throws ProcessingException   if the content of the message cannot be
    *                               mapped to an entity of the requested type. See {@link Response#readEntity(GenericType)}
    * @throws IllegalStateException if the entity is not backed by an input stream,
    *                               the response has been {@link Response#close() closed} already,
    *                               or if the entity input stream has been fully consumed already and has
    *                               not been buffered prior consuming. See {@link Response#readEntity(GenericType)}
    */
   public static <E> E readErrorBody(Response response, TypeReference<E> errorType) {
      if (response.getStatus() > 399) {
         return response.readEntity(new GenericType<>(errorType.getType()));
      } else {
         return null;
      }
   }

   /**
    * Read json body of the given {@code response} as {@code errorType}.
    *
    * @param response the response that produced an error.
    * @param errorType the expected type of the data in the response body
    * @param <E> the type the response body is converted to
    * @return the response body converted to the desired {@code errorType} or {@code null} if the response is neither a
    *         client error (4xx) nor a server error (5xx).
    * @throws ProcessingException   if the content of the message cannot be
    *                               mapped to an entity of the requested type. See {@link Response#readEntity(GenericType)}
    * @throws IllegalStateException if the entity is not backed by an input stream,
    *                               the response has been {@link Response#close() closed} already,
    *                               or if the entity input stream has been fully consumed already and has
    *                               not been buffered prior consuming. See {@link Response#readEntity(GenericType)}
    */
   public static <E> E readErrorBody(Response response, GenericType<E> errorType) {
      if (response.getStatus() > 399) {
         return response.readEntity(errorType);
      } else {
         return null;
      }
   }

   /**
    * Read json body of the request encapsulated in the given {@link ClientRequestException} as {@code errorType}.
    *
    * @param e the error occurred when requesting a resource.
    * @param errorType the expected type of the data in the response body
    * @param <E> the type the response body is converted to
    * @return the response body converted to the desired {@code errorType} or {@code null} if the given
    *         {@link ClientRequestException} does not contain a {@link WebApplicationException} with a {@link Response}
    *         that is a client error (4xx) or a server error (5xx).
    * @throws ProcessingException   if the content of the message cannot be
    *                               mapped to an entity of the requested type. See {@link Response#readEntity(Class)}
    * @throws IllegalStateException if the entity is not backed by an input stream,
    *                               the response has been {@link Response#close() closed} already,
    *                               or if the entity input stream has been fully consumed already and has
    *                               not been buffered prior consuming. See {@link Response#readEntity(Class)}
    */
   public static <E> E readErrorBody(ClientRequestException e, Class<E> errorType) {
      return Optional.ofNullable(e.getCause())
            .map(WebApplicationException::getResponse)
            .map(r -> readErrorBody(r, errorType))
            .orElse(null);
   }

   /**
    * Read json body of the request encapsulated in the given {@link ClientRequestException} as {@code errorType}.
    *
    * @param e the error occurred when requesting a resource.
    * @param errorType the expected type of the data in the response body
    * @param <E> the type the response body is converted to
    * @return the response body converted to the desired {@code errorType} or {@code null} if the given
    *         {@link ClientRequestException} does not contain a {@link WebApplicationException} with a {@link Response}
    *         that is a client error (4xx) or a server error (5xx).
    * @throws ProcessingException   if the content of the message cannot be
    *                               mapped to an entity of the requested type. See {@link Response#readEntity(GenericType)}
    * @throws IllegalStateException if the entity is not backed by an input stream,
    *                               the response has been {@link Response#close() closed} already,
    *                               or if the entity input stream has been fully consumed already and has
    *                               not been buffered prior consuming. See {@link Response#readEntity(GenericType)}
    */
   public static <E> E readErrorBody(ClientRequestException e, TypeReference<E> errorType) {
      return Optional.ofNullable(e.getCause())
            .map(WebApplicationException::getResponse)
            .map(r -> readErrorBody(r, errorType))
            .orElse(null);
   }

   /**
    * Read json body of the request encapsulated in the given {@link ClientRequestException} as {@code errorType}.
    *
    * @param e the error occurred when requesting a resource.
    * @param errorType the expected type of the data in the response body
    * @param <E> the type the response body is converted to
    * @return the response body converted to the desired {@code errorType} or {@code null} if the given
    *         {@link ClientRequestException} does not contain a {@link WebApplicationException} with a {@link Response}
    *         that is a client error (4xx) or a server error (5xx).
    * @throws ProcessingException   if the content of the message cannot be
    *                               mapped to an entity of the requested type. See {@link Response#readEntity(GenericType)}
    * @throws IllegalStateException if the entity is not backed by an input stream,
    *                               the response has been {@link Response#close() closed} already,
    *                               or if the entity input stream has been fully consumed already and has
    *                               not been buffered prior consuming. See {@link Response#readEntity(GenericType)}
    */
   public static <E> E readErrorBody(ClientRequestException e, GenericType<E> errorType) {
      return Optional.ofNullable(e.getCause())
            .map(WebApplicationException::getResponse)
            .map(r -> readErrorBody(r, errorType))
            .orElse(null);
   }

   /**
    * <p>
    *    Wrapped around a request from a
    *    {@link org.sdase.commons.client.jersey.builder.PlatformClientBuilder#buildGenericClient(String) platform generic client}
    *    of a
    *    {@link org.sdase.commons.client.jersey.builder.ExternalClientBuilder#buildGenericClient(String) external generic client}
    *    to catch any {@link WebApplicationException} that is thrown during the request and convert it into
    *    {@link ClientRequestException} that is properly handled by {@link javax.ws.rs.ext.ExceptionMapper}s for the
    *    incoming request.
    * </p>
    * <p>
    *    This conversion is not required for
    *    {@link org.sdase.commons.client.jersey.builder.PlatformClientBuilder#api(Class) platform api clients} or
    *    {@link org.sdase.commons.client.jersey.builder.ExternalClientBuilder#api(Class) external api clients} from
    *    interfaces. They do this conversion automatically.
    * </p>
    *
    * @param request the request lambda that is executing the request, e.g.
    * <pre>
    * <code> WebTarget carsTarget = clientBuilder.buildGenericClient("cars").target(CARS_TARGET)<br/>
    * Car car = ClientErrorUtil.convertExceptions(<br/>
    *         () -> carsTarget.path("api").path("cars").path("123")<br/>
    *                 .request(MediaType.APPLICATION_JSON)<br/>
    *                 .get(Car.class)<br/>
    *   );
    * </code>
    * </pre>
    * @param <T> the expected return type of the request body
    *
    * @return the response body converted to the desired type
    *
    * @throws ClientRequestException if any {@link WebApplicationException} is thrown
    */
   public static <T> T convertExceptions(ResponseSupplier<T> request) {
      try {
         return request.get();
      }
      catch (WebApplicationException e) {
         throw new ClientRequestException(e);
      }
   }

   /**
    * A supplier that executes a request.
    *
    * @param <T> the expected response type
    */
   @FunctionalInterface
   public interface ResponseSupplier<T> extends Supplier<T> {

      /**
       * @return the value that is read from the response body, see
       *       <ul>
       *          <li>{@link javax.ws.rs.client.Invocation.Builder#get(Class)}</li>
       *          <li>{@link javax.ws.rs.client.Invocation.Builder#get(GenericType)}</li>
       *          <li>{@link javax.ws.rs.client.Invocation.Builder#post(Entity, Class)}</li>
       *          <li>{@link javax.ws.rs.client.Invocation.Builder#post(Entity, GenericType)}</li>
       *          <li>{@link javax.ws.rs.client.Invocation.Builder#delete(Class)}</li>
       *          <li>{@link javax.ws.rs.client.Invocation.Builder#delete(GenericType)}</li>
       *       </ul>
       * @throws WebApplicationException if the request failed
       */
      @Override
      T get();
   }



}
