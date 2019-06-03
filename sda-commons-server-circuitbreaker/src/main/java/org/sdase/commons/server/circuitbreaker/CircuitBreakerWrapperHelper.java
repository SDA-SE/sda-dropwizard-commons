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

/**
 * Helper class for wrapping calls on classes with a circuit breaker.
 */
public class CircuitBreakerWrapperHelper {
   private static final Set<String> IGNORED_METHODS = Collections
         .unmodifiableSet(new HashSet<>(Arrays.asList("finalize", "equals", "hashCode", "toString", "$jacocoInit")));

   private CircuitBreakerWrapperHelper() {
      // no instances
   }

   /**
    * Wraps all calls on target using a circuit breaker.
    *
    * @param target
    *           The target to wrap. Either a class, or a final class that
    *           implements an interface (e.g. a Jersey client proxy).
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
      if (Modifier.isFinal(targetClass.getModifiers()) || !hasDefaultConstructor(targetClass)) {
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

      try {
         // If you receive a method not found exception here, you might miss a
         // default constructor in the target class. As an alternative you can
         // also extract an interface and use it here.
         Proxy proxy = (Proxy) proxyClass.newInstance();
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
      } catch (InstantiationException | IllegalAccessException e) {
         throw new IllegalStateException(e);
      }
   }

   private static boolean hasDefaultConstructor(Class<?> clazz) {
      try {
         clazz.getConstructor();
         return true;
      } catch (NoSuchMethodException e) {
         return false;
      }
   }
}
