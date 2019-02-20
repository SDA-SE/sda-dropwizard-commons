package org.sdase.commons.server.auth.error;

import org.sdase.commons.shared.api.error.ApiError;

/**
 * Exception to be thrown when JWT verification failed. This exception leads to a {@code 401 Unauthorized} response
 * code. The {@link RuntimeException#getMessage() message} is used as {@link ApiError#getTitle() title} in the response
 * body.
 */
public class JwtAuthException extends RuntimeException {

   public JwtAuthException(String message) {
      super(message);
   }

   public JwtAuthException(String message, Throwable cause) {
      super(message, cause);
   }

   public JwtAuthException(Throwable cause) {
      super(cause);
   }
}
