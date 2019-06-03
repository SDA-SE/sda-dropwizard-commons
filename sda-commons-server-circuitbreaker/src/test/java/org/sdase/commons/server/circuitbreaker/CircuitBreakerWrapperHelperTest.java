package org.sdase.commons.server.circuitbreaker;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Proxy;

import org.junit.Test;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

public class CircuitBreakerWrapperHelperTest {

   @Test
   public void shouldWrapClassWithDefaultConstructor() {
      // This is the happy path, but will probably never happen...
      CircuitBreaker circuitBreaker = CircuitBreakerRegistry.ofDefaults().circuitBreaker("wrap");
      Simple target = new SimpleImpl();

      Simple wrapped = CircuitBreakerWrapperHelper.wrapWithCircuitBreaker(target, circuitBreaker);

      wrapped.check();

      assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(1);
   }

   @Test
   public void shouldWrapClassWithoutDefaultConstructor() {
      // This requires to have a default constructor just for creating the proxy
      // (similar to WELD):
      CircuitBreaker circuitBreaker = CircuitBreakerRegistry.ofDefaults().circuitBreaker("wrap");
      Complex target = new Complex(42);

      Complex wrapped = CircuitBreakerWrapperHelper.wrapWithCircuitBreaker(target, circuitBreaker);

      wrapped.check();

      assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(1);
   }

   @Test
   public void shouldWrapClassWithoutDefaultConstructorViaInterface() {
      // Alternative to the default constructor is using an interface that
      // specifies the methods of the class:
      CircuitBreaker circuitBreaker = CircuitBreakerRegistry.ofDefaults().circuitBreaker("wrap");
      Simple target = new ComplexImpl(42);

      Simple wrapped = CircuitBreakerWrapperHelper.wrapWithCircuitBreaker(target, circuitBreaker);

      wrapped.check();

      assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(1);
   }

   @Test
   public void shouldWrapDynamicProxy() {
      // We might want to proxy an existing proxy, e.g. a Jersey proxy client,
      // based on an invocation handler:
      CircuitBreaker circuitBreaker = CircuitBreakerRegistry.ofDefaults().circuitBreaker("wrap");
      Simple target = (Simple) Proxy
            .newProxyInstance(getClass().getClassLoader(), new Class[] { Simple.class },
                  (proxy, method, methodArgs) -> {
                     if (method.getName().equals("check")) {
                        return 42;
                     } else {
                        throw new UnsupportedOperationException("Unsupported method: " + method.getName());
                     }
                  });

      Simple wrapped = CircuitBreakerWrapperHelper.wrapWithCircuitBreaker(target, circuitBreaker);

      wrapped.check();

      assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(1);
   }

   interface Simple {
      int check();
   }

   static class SimpleImpl implements Simple {

      public int check() {
         return 42;
      }
   }

   static class Complex {

      private final int value;

      public Complex() {
         value = 0;
      }

      public Complex(int value) {
         this.value = value;
      }

      public int check() {
         return value;
      }
   }

   static class ComplexImpl implements Simple {

      private final int value;

      public ComplexImpl(int value) {
         this.value = value;
      }

      public int check() {
         return value;
      }
   }
}
