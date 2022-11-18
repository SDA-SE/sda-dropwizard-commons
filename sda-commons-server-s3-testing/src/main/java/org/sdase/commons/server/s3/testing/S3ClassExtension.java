package org.sdase.commons.server.s3.testing;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.sdase.commons.server.s3.testing.builder.ContentObject;
import org.sdase.commons.server.s3.testing.builder.FileObject;
import org.sdase.commons.server.s3.testing.builder.MockObject;
import org.sdase.commons.server.s3.testing.builder.StreamObject;

/**
 * JUnit 5 extension for running a AWS S3-compatible object storage alongside the (integration)
 * tests. Use {@link #getEndpoint()} to retrieve the endpoint URL to connect to.
 *
 * <p>Example usage:
 *
 * <pre>
 *   &#64;RegisterExtension
 *   static final S3ClassExtension S3_EXTENSION = S3ClassExtension.builder().build();
 * </pre>
 */
public class S3ClassExtension extends S3Mock implements BeforeAllCallback, AfterAllCallback {

  public S3ClassExtension(final List<String> buckets, final List<MockObject> mockObjects) {
    super(buckets, mockObjects);
  }

  @Override
  public void beforeAll(ExtensionContext context) {
    start();
  }

  @Override
  public void afterAll(ExtensionContext context) {
    stop();
  }

  public void start() {
    super.start();
  }

  public void stop() {
    super.stop();
  }

  //
  // Builder
  //
  public static S3ClassExtension.Builder builder() {
    return new S3ClassExtension.Builder();
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
    public S3ClassExtension.Builder createBucket(String bucketName) {
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
    public S3ClassExtension.Builder putObject(String bucketName, String key, File file) {
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
    public S3ClassExtension.Builder putObject(String bucketName, String key, String content) {
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
    public S3ClassExtension.Builder putObject(String bucketName, String key, InputStream stream) {
      createBucket(bucketName);
      mockObjects.add(new StreamObject(bucketName, key, stream));
      return this;
    }

    public S3ClassExtension build() {
      return new S3ClassExtension(buckets, mockObjects);
    }
  }
}
