package org.sdase.commons.server.cors;

import java.util.ArrayList;
import java.util.List;

public class CorsConfiguration {

   /**
    * <p>
    *    The allowed origins. An empty List if no cross origin requests are allowed. These origins are added to the
    *    {@link org.eclipse.jetty.servlets.CrossOriginFilter} where they are defined as a magic regular expression:
    * </p>
    * <blockquote>
    *    <p>
    *       If an allowed origin contains one or more * characters (for example http://*.domain.com), then "*"
    *       characters are converted to ".*", "." characters are escaped to "\." and the resulting allowed origin
    *       interpreted as a regular expression.
    *    </p>
    *    <p>
    *       Allowed origins can therefore be more complex expressions such as https?://*.domain.[a-z]{3} that matches
    *       http or https, multiple subdomains and any 3 letter top-level domain (.com, .net, .org, etc.).
    *    </p>
    * </blockquote>
    */
   private List<String> allowedOrigins = new ArrayList<>();

   public List<String> getAllowedOrigins() {
      if (allowedOrigins == null) {
         return new ArrayList<>();
      }
      return allowedOrigins;
   }

   public void setAllowedOrigins(List<String> allowedOrigins) {
      this.allowedOrigins = allowedOrigins;
   }

}
