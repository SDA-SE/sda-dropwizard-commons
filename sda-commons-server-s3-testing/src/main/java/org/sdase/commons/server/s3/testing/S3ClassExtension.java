package org.sdase.commons.server.s3.testing;

import static org.assertj.core.api.Assertions.assertThat;

import com.robothy.s3.jupiter.LocalS3Endpoint;
import com.robothy.s3.jupiter.extensions.LocalS3Extension;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.sdase.commons.server.s3.testing.builder.ContentObject;
import org.sdase.commons.server.s3.testing.builder.FileObject;
import org.sdase.commons.server.s3.testing.builder.MockObject;
import org.sdase.commons.server.s3.testing.builder.StreamObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;

/**
 * JUnit 5 extension for running a AWS S3-compatible object storage alongside the (integration)
 * tests. Use {@link #getEndpoint()} to retrieve the endpoint URL to connect to.
 *
 * <p>Example usage:
 *
 * <pre>
 *   &#64;RegisterExtension
 *   static final S3ClassExtension S3_EXTENSION = new S3ClassExtension();
 * </pre>
 */
public class S3ClassExtension extends LocalS3Extension {

  private static final Logger LOG = LoggerFactory.getLogger(S3ClassExtension.class);

  private final Set<String> buckets;
  private final List<MockObject> mockObjects;
  private LocalS3Endpoint classS3Endpoint;

  S3ClassExtension(Set<String> buckets, List<MockObject> mockObjects) {
    this.buckets = buckets;
    this.mockObjects = mockObjects;
  }

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    super.beforeAll(context);
    Object portAsObject =
        context
            .getStore(ExtensionContext.Namespace.GLOBAL)
            .get(context.getRequiredTestClass() + LOCAL_S3_PORT_STORE_SUFFIX);
    if (portAsObject != null) {
      var port = (int) portAsObject;
      classS3Endpoint = new LocalS3Endpoint(port);
      resetAll();
    }
  }

  public void resetAll() {
    try (S3Client client = newClient()) {
      deleteRemoteBuckets(client);
      createBuckets(client);
      createMockObjects(client);
    }
  }

  private void deleteRemoteBuckets(S3Client client) {
    var listBucketResponse = client.listBuckets();
    listBucketResponse
        .buckets()
        .forEach(
            bucketName -> {
              LOG.info("Delete bucket: {}", bucketName.name());
              deleteObjectsFromBucket(client, bucketName.name());
              var response =
                  client.deleteBucket(
                      DeleteBucketRequest.builder().bucket(bucketName.name()).build());
              if (!response.sdkHttpResponse().isSuccessful()) {
                LOG.info("Could not delete bucket: {}", bucketName.name());
              }
            });
  }

  private void deleteObjectsFromBucket(S3Client client, String bucketName) {
    var response = client.listObjects(ListObjectsRequest.builder().bucket(bucketName).build());
    response.contents().stream()
        .map(
            s3Object ->
                DeleteObjectRequest.builder().bucket(bucketName).key(s3Object.key()).build())
        .forEach(client::deleteObject);
    if (!response.sdkHttpResponse().isSuccessful()) {
      LOG.info("Could not delete objects in bucket: {}", bucketName);
    }
  }

  private void createBuckets(S3Client client) {
    buckets.stream()
        .filter(b -> b != null && !b.isBlank())
        .forEach(
            bucketName -> {
              LOG.info("Create bucket: {}", bucketName);
              var response =
                  client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
              assertThat(response.sdkHttpResponse().isSuccessful()).isTrue();
            });
  }

  private void createMockObjects(S3Client client) {
    mockObjects.forEach(mockObject -> mockObject.putObject(client));
  }

  /**
   * @return a new S3 client
   * @deprecated Use {@link #newClient()} instead.
   */
  @Deprecated
  public S3Client getClient() {
    return newClient();
  }

  /**
   * @return a new S3 client
   */
  public S3Client newClient() {
    return S3Client.builder()
        .endpointOverride(URI.create(getEndpoint()))
        .region(Region.of("local"))
        .credentialsProvider(AnonymousCredentialsProvider.create())
        .forcePathStyle(true)
        .httpClient(UrlConnectionHttpClient.builder().build())
        .build();
  }

  @Override
  public void beforeEach(ExtensionContext context) {
    // nothing to do
  }

  public String getEndpoint() {
    return classS3Endpoint.endpoint();
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
     * @param stream the content to store as stream
     * @return The builder
     */
    public Builder putObject(String bucketName, String key, InputStream stream) {
      createBucket(bucketName);
      mockObjects.add(new StreamObject(bucketName, key, stream));
      return this;
    }

    public S3ClassExtension build() {
      return new S3ClassExtension(buckets, mockObjects);
    }
  }
}
