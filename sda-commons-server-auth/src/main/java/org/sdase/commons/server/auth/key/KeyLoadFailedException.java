package org.sdase.commons.server.auth.key;

/**
 * Thrown when {@link KeySource} fails to load keys.
 */
public class KeyLoadFailedException extends RuntimeException {

   public KeyLoadFailedException() {
   }

   public KeyLoadFailedException(String message) {
      super(message);
   }

   public KeyLoadFailedException(String message, Throwable cause) {
      super(message, cause);
   }

   public KeyLoadFailedException(Throwable cause) {
      super(cause);
   }

   public KeyLoadFailedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
      super(message, cause, enableSuppression, writableStackTrace);
   }
}
