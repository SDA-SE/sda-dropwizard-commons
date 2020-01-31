package org.sdase.commons.server.opentracing.client;

import io.opentracing.Tracer;
import io.opentracing.contrib.jaxrs2.client.ClientSpanDecorator;
import io.opentracing.contrib.jaxrs2.client.ClientTracingFeature;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.client.Client;

public class ClientTracingUtil {

  private ClientTracingUtil() {
    // Hide constructor
  }

  public static void registerTracing(Client client, Tracer tracer) {
    List<ClientSpanDecorator> clientDecorators = new ArrayList<>();
    clientDecorators.add(ClientSpanDecorator.STANDARD_TAGS);
    clientDecorators.add(new CustomClientSpanDecorator());

    // This registers both a filter that instruments request, but also injects
    // the span and trace
    // headers into the request. Passing the headers to an external service
    // might not be desired.
    //
    // At the moment we can't have different behavior, without implementing
    // more by our self. But on the other side, the headers do no harm. They
    // neither expose secrets nor should they break server behavior.
    client.register(
        new ClientTracingFeature.Builder(tracer).withDecorators(clientDecorators).build());
  }
}
