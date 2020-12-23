package org.sdase.commons.server.s3.testing;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import io.findify.s3mock.S3Mock;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.extension.*;
import org.sdase.commons.server.s3.testing.builder.ContentObject;
import org.sdase.commons.server.s3.testing.builder.FileObject;
import org.sdase.commons.server.s3.testing.builder.MockObject;
import org.sdase.commons.server.s3.testing.builder.StreamObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JUnit 5 extension for running a AWS S3-compatible object storage alongside the (integration)
 * tests. Use {@link #getEndpoint()} to retrieve the endpoint URL to connect to.
 *
 * <p>Example usage:
 *
 * <pre>
 *   &#64;RegisterExtension
 *   static final S3Extension S3_EXTENSION = S3Extension.builder().build();
 * </pre>
 */
public class S3Extension implements BeforeAllCallback, AfterAllCallback {

  private static final Logger LOGGER = LoggerFactory.getLogger(S3Extension.class);
  private final List<String> buckets;
  private final List<MockObject> mockObjects;

  private S3Mock s3Mock;
  private Integer port;

  public S3Extension(final List<String> buckets, final List<MockObject> mockObjects) {
    this.buckets = buckets;
    this.mockObjects = mockObjects;
  }

  @Override
  public void beforeAll(ExtensionContext context) {
    port = getFreePort();
    s3Mock = new S3Mock.Builder().withInMemoryBackend().withPort(port).build();
    s3Mock.start();
    LOGGER.info("Started S3 Mock at {}", getEndpoint());

    AmazonS3 s3Client = getClient();
    initializeObjects(s3Client);
  }

  @Override
  public void afterAll(ExtensionContext context) {
    s3Mock.stop();
    LOGGER.info("Stopped S3 Mock");
  }

  /**
   * Returns the URL to the S3 Mock endpoint.
   *
   * @return A full URL containing scheme, host and port.
   */
  public String getEndpoint() {
    return "http://localhost:" + getPort();
  }

  /**
   * Returns the port where to S3 Mock is listening on.
   *
   * @return The random port.
   */
  public int getPort() {
    if (port == null) {
      throw new IllegalStateException("Extension not started yet, port not available");
    }

    return port;
  }

  /**
   * Clears all buckets and objects from the store. Afterwards restores all buckets and object
   * specified during creation.
   */
  public void resetAll() {
    AmazonS3 s3Client = getClient();

    s3Client
        .listBuckets()
        .forEach(
            bucket -> {
              s3Client
                  .listObjects(bucket.getName())
                  .getObjectSummaries()
                  .forEach(o -> s3Client.deleteObject(bucket.getName(), o.getKey()));
              s3Client.deleteBucket(bucket.getName());
            });

    initializeObjects(s3Client);
  }

  /**
   * Creates a client to manipulate the object storage during tests.
   *
   * @return get the client from the AWS SDK
   */
  public AmazonS3 getClient() {
    return AmazonS3ClientBuilder.standard()
        .withEndpointConfiguration(
            new AwsClientBuilder.EndpointConfiguration(
                getEndpoint(), Regions.DEFAULT_REGION.getName()))
        .withPathStyleAccessEnabled(true)
        .build();
  }

  private void initializeObjects(AmazonS3 s3Client) {
    buckets.forEach(
        bucketName -> {
          if (!s3Client.doesBucketExistV2(bucketName)) {
            s3Client.createBucket(bucketName);
          }
        });
    mockObjects.forEach(o -> o.putObject(s3Client));
  }

  private int getFreePort() {
    try (ServerSocket socket = new ServerSocket(0)) {
      socket.setReuseAddress(true);
      return socket.getLocalPort();
    } catch (IOException e) {
      // Use a fixed port number as a fallback if an error occurs
      return 28301;
    }
  }

  //
  // Builder
  //
  public static S3Extension.Builder builder() {
    return new S3Extension.Builder();
  }

  public static final class Builder {
    private final List<String> buckets = new ArrayList<>();
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
    public S3Extension.Builder createBucket(String bucketName) {
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
    public S3Extension.Builder putObject(String bucketName, String key, File file) {
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
    public S3Extension.Builder putObject(String bucketName, String key, String content) {
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
    public S3Extension.Builder putObject(String bucketName, String key, InputStream stream) {
      createBucket(bucketName);
      mockObjects.add(new StreamObject(bucketName, key, stream));
      return this;
    }

    public S3Extension build() {
      return new S3Extension(buckets, mockObjects);
    }
  }
}
