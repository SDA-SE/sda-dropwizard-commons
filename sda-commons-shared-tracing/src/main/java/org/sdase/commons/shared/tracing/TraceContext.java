package org.sdase.commons.shared.tracing;

import java.util.UUID;
import org.slf4j.MDC;

public class TraceContext {

  public static class Data {
    private String traceToken;

    public String getTraceToken() {
      return traceToken;
    }

    public Data setTraceToken(String traceToken) {
      this.traceToken = traceToken;
      return this;
    }
  }

  private static final ThreadLocal<Data> DATA_HOLDER = new ThreadLocal<>();

  public static String createNewTraceToken() {
    Data data = getDataForCurrentThread();
    String traceToken = data.getTraceToken();
    if (traceToken == null) {
      traceToken = UUID.randomUUID().toString();
      data.setTraceToken(traceToken);
    }

    putTraceTokenIntoLoggingContext(traceToken);
    return traceToken;
  }

  private static Data getDataForCurrentThread() {
    Data result = DATA_HOLDER.get();
    if (result == null) {
      result = new Data();
      DATA_HOLDER.set(result);
    }
    return result;
  }

  public static void storeTraceToken(String traceToken) {
    getDataForCurrentThread().setTraceToken(traceToken);
    putTraceTokenIntoLoggingContext(traceToken);
  }

  public static String getCurrentOrCreateNewTraceToken() {
    String traceToken = getDataForCurrentThread().getTraceToken();
    if (traceToken == null) {
      traceToken = createNewTraceToken();
    }
    return traceToken;
  }

  public static void clear() {
    DATA_HOLDER.remove();
  }

  private static void putTraceTokenIntoLoggingContext(String traceToken) {
    if (MDC.getMDCAdapter() != null) {
      MDC.put(RequestTracing.TOKEN_MDC_KEY, traceToken);
    }
  }
}
