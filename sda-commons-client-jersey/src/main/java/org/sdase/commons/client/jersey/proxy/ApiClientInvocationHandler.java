package org.sdase.commons.client.jersey.proxy;

import org.sdase.commons.client.jersey.error.ClientRequestException;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class ApiClientInvocationHandler implements InvocationHandler {

   private Object delegate;

   /**
    * Creates a proxy around the given {@code jerseyClientProxy} that wraps all {@link WebApplicationException}s in
    * {@link ClientRequestException}s.
    *
    * @param apiInterface the client interface
    * @param jerseyClientProxy the proxy instance build by
    *       {@code WebResourceFactory.newResource(Class<C> resourceInterface, ...)}
    * @param <T> the client interface
    * @return a proxy around the {@code jerseyClientProxy}
    */
   public static <T> T createProxy(Class<T> apiInterface, T jerseyClientProxy) {
      if (!apiInterface.isInterface()) {
         throw new IllegalArgumentException("apiInterface is not an interface but '" + apiInterface.getName() + "'");
      }
      ApiClientInvocationHandler clientInvocationHandler = new ApiClientInvocationHandler(jerseyClientProxy);
      ClassLoader classLoader = ApiClientInvocationHandler.class.getClassLoader();
      Class[] proxyDefinition = { apiInterface };
      //noinspection unchecked
      return (T) Proxy.newProxyInstance(classLoader, proxyDefinition, clientInvocationHandler);
   }

   private ApiClientInvocationHandler(Object delegate) {
      this.delegate = delegate;
   }

   @Override
   public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      try {
         return method.invoke(delegate, args);
      }
      catch (InvocationTargetException invocationTargetException) {
         Throwable cause = invocationTargetException.getCause();
         if (cause instanceof WebApplicationException) {
            throw new ClientRequestException(cause);
         } else if (cause instanceof ProcessingException) {
            throw new ClientRequestException(cause);
         } else {
            throw cause;
         }
      }
   }
}
