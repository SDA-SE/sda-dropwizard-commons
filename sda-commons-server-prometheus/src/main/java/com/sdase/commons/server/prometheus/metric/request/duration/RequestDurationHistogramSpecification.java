package com.sdase.commons.server.prometheus.metric.request.duration;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Histogram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ResourceInfo;
import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * The central definition of the response duration histogram. This is the definition of the response duration histogram
 * as it should be provided by all SDA services to measure request durations with Prometheus.
 */
public class RequestDurationHistogramSpecification {

   private static final Logger LOG = LoggerFactory.getLogger(RequestDurationHistogramSpecification.class);

   /**
    * The consumer token filter is advised to store the name of the consumer using this key in request context
    * attributes. The consumer token filter should derive the consumer name from the consumer token received in the
    * request headers .
    */
   private static final String CONSUMER_NAME_KEY = "Consumer-Name";

   /**
    * The histogram name as it is published to Prometheus.
    */
   private static final String HISTOGRAM_NAME = "http_request_duration_seconds";

   /**
    * The help message description that describes the Histogram.
    */
   private static final String DESCRIPTION = "Duration of HTTP requests in seconds.";

   /**
    * The labels added by {@code RequestDurationHistogram}. The labels and their order have to be aligned with the
    * values created in
    * {@link #createLabelValuesForCurrentRequest(ResourceInfo, ContainerRequestContext, ContainerResponseContext)}
    */
   private static final String[] LABELS = {
         // the name of the method that handled the request
         "implementing_method",
         // the http method of the request
         "http_method",
         // the request resource path with path parameter names instead of the actual values
         "resource_path",
         // the status code of the response
         "status_code",
         // the name of the consumer derived from the request attribute defined in #CONSUMER_NAME_KEY
         "consumer_name"
   };

   private Histogram requestDurationHistogram;

   /**
    * Creates and registers a new {@link Histogram} matching the specification of this
    * {@code RequestDurationHistogramSpecification} instance. <strong>Note that there should be only one registered
    * instance of this type in the application.</strong>
    */
   public RequestDurationHistogramSpecification() {
      this.requestDurationHistogram = createAndRegister();
   }

   /**
    * Unregisters the histogram. Should be called when the context is closed.
    */
   public void unregister() {
      CollectorRegistry.defaultRegistry.unregister(requestDurationHistogram);
   }

   /**
    * Observes the given request duration and adds the defined labels according to the request and response context
    * information.
    *
    * @param requestDurationSeconds the duration of the current request in seconds
    * @param resourceInfo the {@link ResourceInfo} of the current request to determine values for the labels
    * @param requestContext the context of the current request to determine values for the labels
    * @param responseContext the context of the current response to determine values for the labels
    */
   void observe(double requestDurationSeconds,
                       ResourceInfo resourceInfo,
                       ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) {
      String[] labelValues = createLabelValuesForCurrentRequest(
            resourceInfo, requestContext, responseContext);
      requestDurationHistogram.labels(labelValues).observe(requestDurationSeconds);
   }

   /**
    * Creates all values for the labels required by the histogram in appropriate order.
    *
    * @param resourceInfo the {@link ResourceInfo} of the current request
    * @param requestContext the context of the current request
    * @param responseContext the context of the current response
    * @return all values for the labels in the order they are registered in the histogram
    */
   private String[] createLabelValuesForCurrentRequest(
         ResourceInfo resourceInfo,
         ContainerRequestContext requestContext,
         ContainerResponseContext responseContext) {
      // the number of values and their order has to be aligned with the defined #LABELS
      return new String[] {
            getImplementingMethod(resourceInfo),
            getHttpMethod(requestContext),
            getResourcePath(requestContext),
            getStatusCode(responseContext),
            getConsumerName(requestContext)
      };
   }

   /**
    * Builds the {@link Histogram} to measure request duration and registers it.
    *
    * @return the registered {@link Histogram}
    */
   private Histogram createAndRegister() {
      Histogram.Builder requestDurationHistogramBuilder = Histogram.build()
            .name(HISTOGRAM_NAME)
            .labelNames(LABELS)
            .help(DESCRIPTION);
      Histogram histogram = requestDurationHistogramBuilder.create();
      LOG.debug("Created Histogram {}", HISTOGRAM_NAME);
      CollectorRegistry.defaultRegistry.register(histogram);
      LOG.debug("Registered Histogram {}", HISTOGRAM_NAME);
      return histogram;
   }

   private String getHttpMethod(ContainerRequestContext requestContext) {
      return requestContext.getMethod();
   }

   private String getImplementingMethod(ResourceInfo resourceInfo) {
      return resourceInfo.getResourceMethod().getName();
   }

   private String getResourcePath(ContainerRequestContext requestContext) {
      String path = path(requestContext.getUriInfo().getRequestUri());
      return restorePathParamPlaceholders(path, requestContext.getUriInfo().getPathParameters());
   }

   private String getStatusCode(ContainerResponseContext responseContext) {
      return String.valueOf(responseContext.getStatus());
   }

   private String getConsumerName(ContainerRequestContext requestContext) {
      Object consumerName = requestContext.getProperty(CONSUMER_NAME_KEY);
      return consumerName != null
            ? consumerName.toString()
            : "";
   }

   /**
    * Returns path of given URI. If the first character of path is '/' then it is removed.
    *
    * @param uri
    *           to convert
    * @return path or null
    */
   private static String path(URI uri) {
      String path = uri.getPath();
      if (path != null && path.startsWith("/")) {
         path = path.substring(1);
      }
      return path;
   }


   private static String restorePathParamPlaceholders(String requestPath, Map<String, List<String>> pathParameters) {
      if (requestPath == null) {
         return null;
      }

      String path = requestPath;

      for (Map.Entry<String, List<String>> pathParam : pathParameters.entrySet()) {
         String originalPlaceholder = "{" + pathParam.getKey() + "}";
         for (String actualValue : pathParam.getValue()) {
            path = path.replace(actualValue, originalPlaceholder);
         }
      }

      return path;
   }
}
