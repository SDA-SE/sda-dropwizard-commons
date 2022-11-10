package org.sdase.commons.server.jackson.hal;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An utility class to process and store the invocation information of a method. It also creates and
 * returns a proxy that uses the method invocation handler to process the invocation information.
 */
public class HalLinkInvocationStateUtility {

  private static final Logger LOG = LoggerFactory.getLogger(HalLinkInvocationStateUtility.class);

  private static final ThreadLocal<MethodInvocationState> THREAD_LOCAL_METHOD_INVOCATION_STATE =
      ThreadLocal.withInitial(MethodInvocationState::new);

  // Method invocation handler to process and save the current state of the method invocation
  private static final InvocationHandler METHOD_PROCESSING_INVOCATION_HANDLER =
      (proxy, method, methodArguments) -> {
        // Get  invocation state from current thread
        final MethodInvocationState methodInvocationState =
            THREAD_LOCAL_METHOD_INVOCATION_STATE.get();
        final String methodName = method.getName();
        methodInvocationState.setInvokedMethod(methodName);
        LOG.debug("Last invoked method '{}' added to current invocation state", methodName);
        // Process annotated query and path parameters
        processParams(methodInvocationState, methodArguments, method.getParameters());
        methodInvocationState.processed();
        // Do nothing
        return null;
      };

  private HalLinkInvocationStateUtility() {}

  /**
   * Creates and returns a proxy instance based on the passed type parameter with a method
   * invocation handler, which processes and saves the needed method invocation information in the
   * current thread. Parameters in the afterwards called method of the proxy will be used to resolve
   * the URI template of the corresponding method. It should be ensured that the parameters are
   * annotated with {@linkplain PathParam} or with {@linkplain QueryParam}. The passed class type
   * must represent interfaces, not classes or primitive types.
   *
   * @param <T> the type parameter based on the passed type. Should not be null {@literal null}.
   * @param type the type on which the method should be invoked.
   * @return the proxy instance
   * @throws HalLinkMethodInvocationException if the proxy instance could not be created
   */
  static <T> T methodOn(Class<T> type) {
    return createProxy(type);
  }

  @SuppressWarnings("unchecked")
  private static <T> T createProxy(Class<T> type) {
    THREAD_LOCAL_METHOD_INVOCATION_STATE.get().setType(type);
    LOG.debug("Class type: '{}' added to the current invocation state", type);
    try {
      return (T)
          Proxy.newProxyInstance(
              type.getClassLoader(), new Class[] {type}, METHOD_PROCESSING_INVOCATION_HANDLER);
    } catch (IllegalArgumentException | SecurityException | NullPointerException e) {
      throw new HalLinkMethodInvocationException(
          String.format("Could not create proxy instance of type '%s' for method invocation", type),
          e);
    }
  }

  private static void processParams(
      MethodInvocationState methodInvocationState,
      Object[] methodArguments,
      Parameter[] parameters) {

    // Correlation between method argument order and annotatedParams order
    for (int i = 0; i < parameters.length; i++) {
      Parameter parameter = parameters[i];
      if (parameter.isAnnotationPresent(PathParam.class)) {
        final PathParam pathParam = parameter.getAnnotation(PathParam.class);
        final Object paramValue = methodArguments[i];
        methodInvocationState.getPathParams().put(pathParam.value(), paramValue);
        LOG.debug(
            "Saved PathParam: '{}' with value: '{}' to current invocation state",
            pathParam.value(),
            paramValue);
      } else if (parameter.isAnnotationPresent(QueryParam.class)) {
        final QueryParam queryParam = parameter.getAnnotation(QueryParam.class);
        final Object paramValue = methodArguments[i];
        if (paramValue != null) {
          methodInvocationState.getQueryParams().put(queryParam.value(), paramValue);
          LOG.debug(
              "Saved QueryParam: '{}' with value: '{}' to current invocation state",
              queryParam.value(),
              paramValue);
        } else {
          LOG.debug("Ignoring parameter {} annotated with {}", i, parameter.getAnnotations());
        }
      }
    }
  }

  /**
   * Load the method invocation state of the current thread.
   *
   * @return the method invocation state
   */
  static MethodInvocationState loadMethodInvocationState() {
    LOG.debug("Load invocation state of current thread");
    return THREAD_LOCAL_METHOD_INVOCATION_STATE.get();
  }

  /** Unload the method invocation state of the current thread. */
  static void unloadMethodInvocationState() {
    LOG.debug("Remove invocation state of current thread");
    THREAD_LOCAL_METHOD_INVOCATION_STATE.remove();
  }

  /** Data class to save method invocation information */
  static class MethodInvocationState {
    private boolean processed = false;
    private Class<?> type;
    private String invokedMethod;
    private final Map<String, Object> pathParams = new HashMap<>();
    private final Map<String, Object> queryParams = new HashMap<>();

    /**
     * Gets type of the class of the invoked method.
     *
     * @return the type
     */
    Class<?> getType() {
      return type;
    }

    /**
     * Sets type of the class of the invoked method.
     *
     * @param type the type
     */
    void setType(Class<?> type) {
      this.type = type;
    }

    /**
     * Gets the method name.
     *
     * @return the invoked method name
     */
    String getInvokedMethod() {
      return invokedMethod;
    }

    /**
     * Sets the method name.
     *
     * @param invokedMethod the invoked method name
     */
    void setInvokedMethod(String invokedMethod) {
      this.invokedMethod = invokedMethod;
    }

    /**
     * Gets the Key-Value pairs of the processed {@linkplain PathParam#value()} and the
     * corresponding method arguments of the invoked method.
     *
     * @return the path params
     */
    Map<String, Object> getPathParams() {
      return pathParams;
    }

    /**
     * Gets the Key-Value pairs of the processed {@linkplain QueryParam#value()} and the
     * corresponding method arguments of the invoked method.
     *
     * @return the query params
     */
    Map<String, Object> getQueryParams() {
      return queryParams;
    }

    /**
     * Returns {@literal true} if the invoked method is processed.
     *
     * @return the boolean
     */
    boolean isProcessed() {
      return processed;
    }

    /** Set the state of the method invocation to processed. */
    void processed() {
      this.processed = true;
    }
  }
}
