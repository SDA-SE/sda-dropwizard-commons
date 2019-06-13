package org.sdase.commons.server.circuitbreaker;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.util.Set;
import javassist.util.proxy.Proxy;
import javassist.util.proxy.ProxyFactory;
import org.objenesis.ObjenesisStd;

/**
 * Helper class for wrapping calls on classes with a circuit breaker.
 */
public class CircuitBreakerWrapperHelper {

   private static final Set<String> IGNORED_METHODS = Collections
         .unmodifiableSet(new HashSet<>(Arrays.asList("finalize", "equals", "hashCode", "toString", "$jacocoInit")));

   private static final ObjenesisStd OBJENESIS = new ObjenesisStd();

   private CircuitBreakerWrapperHelper() {
      // no instances
   }

   /**
    * Wraps all calls on target using a circuit breaker.
    *
    * @param target
    *           The target to wrap. Final classes have to implement an interface.
    *           (e.g. a Jersey client proxy).
    * @param circuitBreaker
    *           The circuit breaker to use.
    * @param <T>
    *           The type of the target.
    * @return A proxy object that forwards all calls to target.
    */
   @SuppressWarnings("unchecked")
   public static <T> T wrapWithCircuitBreaker(T target, CircuitBreaker circuitBreaker) {
      // We can't use InvocationHandler here, as we would like to proxy classes,
      // without requiring an interface. Instead we use javaassist.
      ProxyFactory proxyFactory = new ProxyFactory();

      Class<?> targetClass = target.getClass();
      if (Modifier.isFinal(targetClass.getModifiers())) {
         // We can't wrap final classes. But sadly the proxy objects returned by
         // the Jersey client are final classes. As a workaround, we implement
         // their interfaces instead, which works in this (special) case.
         proxyFactory.setInterfaces(targetClass.getInterfaces());
      } else {
         // A non final class? Perfect! Just derive from it.
         proxyFactory.setSuperclass(targetClass);
      }

      proxyFactory.setFilter(m -> !IGNORED_METHODS.contains(m.getName()));
      Class proxyClass = proxyFactory.createClass();

      // Extract all methods from the target class, to forward calls from the
      // proxy to the target.
      Map<String, Method> methods = new HashMap<>();
      for (Method method : targetClass.getMethods()) {
         methods.put(method.getName(), method);
      }

      Proxy proxy = (Proxy) OBJENESIS.newInstance(proxyClass);

      // Forward all calls to target, but wrap them in the circuit breaker
      proxy.setHandler((self, m, proceed, args) -> circuitBreaker.executeCheckedSupplier(() -> {
         try {
            return methods.get(m.getName()).invoke(target, args);
         } catch (InvocationTargetException e) {
            // Forward all exceptions that happen during forwarding the call
            // to the target.
            throw e.getTargetException();
         }
      }));

      return (T) proxy;
   }
}
