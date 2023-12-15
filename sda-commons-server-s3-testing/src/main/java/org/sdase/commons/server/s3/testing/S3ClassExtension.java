package org.sdase.commons.server.s3.testing;

import com.robothy.s3.jupiter.LocalS3;
import com.robothy.s3.jupiter.LocalS3Endpoint;
import com.robothy.s3.jupiter.extensions.LocalS3Extension;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.sdase.commons.server.s3.testing.builder.ContentObject;
import org.sdase.commons.server.s3.testing.builder.FileObject;
import org.sdase.commons.server.s3.testing.builder.MockObject;
import org.sdase.commons.server.s3.testing.builder.StreamObject;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * JUnit 5 extension for running an AWS S3-compatible object storage alongside the (integration)
 * tests. Use {@link #getEndpoint()} to retrieve the endpoint URL to connect to.
 *
 * <p>Example usage:
 *
 * <pre>
 * &#64;LocalS3
 * class MyS3Test {
 *
 *   &#64;RegisterExtension
 *   &#64;Order(0)
 *   static final S3ClassExtension S3 = S3ClassExtension.builder().createBucket("testbucket").build();
 *
 * }
 * </pre>
 */
public class S3ClassExtension extends LocalS3Extension {

  private final Set<String> buckets;
  private final List<MockObject> mockObjects;
  private LocalS3Endpoint classS3Endpoint;

  private final S3Manager s3Manager = new S3Manager();

  S3ClassExtension(Set<String> buckets, List<MockObject> mockObjects) {
    this.buckets = buckets;
    this.mockObjects = mockObjects;
  }

  @Override
  public void beforeEach(ExtensionContext context) {
    // this class should only be used as class level extension
    // thus, not calling super
  }

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    if (!context.getRequiredTestClass().isAnnotationPresent(LocalS3.class)) {
      throw new IllegalStateException("The test class must be annotated with @LocalS3");
    }

    super.beforeAll(context);
    Object portAsObject =
        context
            .getStore(ExtensionContext.Namespace.GLOBAL)
            .get(context.getRequiredTestClass() + LOCAL_S3_PORT_STORE_SUFFIX);
    if (portAsObject == null) {
      throw new IllegalStateException("Could not determine port of S3 service");
    }

    var port = (int) portAsObject;
    classS3Endpoint = new LocalS3Endpoint(port);
    resetAll();
  }

  public void resetAll() {
    try (S3Client client = newClient()) {
      s3Manager.deleteAllBuckets(client);
      s3Manager.createBuckets(client, buckets);
      s3Manager.createMockObjects(client, mockObjects);
    }
  }

  /**
   * @return a new S3 client
   * @deprecated Use {@link #newClient()} instead.
   */
  @Deprecated(forRemoval = true, since = "6.0.0")
  public S3Client getClient() {
    return newClient();
  }

  /**
   * @return a new S3 client
   */
  public S3Client newClient() {
    var builder = S3Client.builder();
    if (getEndpoint() != null) {
      builder.endpointOverride(URI.create(getEndpoint()));
    }
    return builder
        .region(Region.of("local"))
        .credentialsProvider(AnonymousCredentialsProvider.create())
        .forcePathStyle(true)
        .httpClient(UrlConnectionHttpClient.builder().build())
        .build();
  }

  public String getEndpoint() {
    return classS3Endpoint == null ? null : classS3Endpoint.endpoint();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private final Set<String> buckets = new HashSet<>();
    private final List<MockObject> mockObjects = new ArrayList<>();

    private Builder() {
      // prevent instantiation
    }

    /**
     * Create a bucket on the S3 service for use during testing.
     *
     * @param bucketName the name of the bucket
     * @return The builder.
     */
    public Builder createBucket(String bucketName) {
      buckets.add(bucketName);
      return this;
    }

    /**
     * Put a file as an object in the S3 service for use during testing. Automatically creates a
     * bucket.
     *
     * @param bucketName the name of the bucket
     * @param key the key to store the file in
     * @param file the content to store as file
     * @return The builder.
     */
    public Builder putObject(String bucketName, String key, File file) {
      createBucket(bucketName);
      mockObjects.add(new FileObject(bucketName, key, file));
      return this;
    }

    /**
     * Put a string as an object in the S3 service for use during testing. Automatically creates a
     * bucket.
     *
     * @param bucketName the name of the bucket
     * @param key the key to store the file in
     * @param content the content to store as string
     * @return The builder.
     */
    public Builder putObject(String bucketName, String key, String content) {
      createBucket(bucketName);
      mockObjects.add(new ContentObject(bucketName, key, content));
      return this;
    }

    /**
     * Put an input stream as an object in the S3 service for use during testing. Automatically
     * creates a bucket.
     *
     * @param bucketName the name of the bucket
     * @param key the key to store the file in
     * @param inputStreamSupplier the content to store
     * @return The builder
     */
    public Builder putObject(
        String bucketName, String key, Supplier<InputStream> inputStreamSupplier) {
      createBucket(bucketName);
      mockObjects.add(new StreamObject(bucketName, key, inputStreamSupplier));
      return this;
    }

    public S3ClassExtension build() {
      return new S3ClassExtension(buckets, mockObjects);
    }
  }
}
