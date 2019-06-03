package org.sdase.commons.server.circuitbreaker.builder;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;

public interface CircuitBreakerFinalBuilder {

   /**
    * Create a new circuit breaker instance.
    *
    * @return A new {@code CircuitBreaker}
    */
   CircuitBreaker build();

   /**
    * Wrap target with a circuit breaker
    * 
    * @param target
    *           The target to wrap. Make sure that the target either has a
    *           default constructor, or use an interface.
    * @param <U>
    *           The type of the target
    * @return A proxy object that passes all calls to target, but wraps them in
    *         a circuit breaker.
    */
   <U> U wrap(U target);
}
