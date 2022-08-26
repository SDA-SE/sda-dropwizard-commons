package org.sdase.commons.server.s3;

import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.Request;
import com.amazonaws.Response;
import com.amazonaws.handlers.HandlerContextKey;
import com.amazonaws.handlers.RequestHandler2;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.awssdk.v1_11.AwsSdkTelemetry;

public class TracingRequestHandler extends RequestHandler2 {
  private final RequestHandler2 delegate;

  public TracingRequestHandler(OpenTelemetry openTelemetry) {
    delegate =
        AwsSdkTelemetry.builder(openTelemetry)
            .setCaptureExperimentalSpanAttributes(true)
            .build()
            .newRequestHandler();
  }

  public static final HandlerContextKey<Scope> SCOPE =
      new HandlerContextKey<>(Scope.class.getName());

  @Override
  public void beforeRequest(Request<?> request) {
    delegate.beforeRequest(request);
  }

  @Override
  public AmazonWebServiceRequest beforeMarshalling(AmazonWebServiceRequest request) {
    return delegate.beforeMarshalling(request);
  }

  @Override
  public void afterResponse(Request<?> request, Response<?> response) {
    delegate.afterResponse(request, response);
  }

  @Override
  public void afterError(Request<?> request, Response<?> response, Exception e) {
    delegate.afterError(request, response, e);
  }
}
