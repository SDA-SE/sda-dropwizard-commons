package org.sdase.commons.server.s3.testing;

import com.amazonaws.services.s3.AmazonS3;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.rules.ExternalResource;
import org.sdase.commons.server.s3.testing.builder.ContentObject;
import org.sdase.commons.server.s3.testing.builder.FileObject;
import org.sdase.commons.server.s3.testing.builder.MockObject;
import org.sdase.commons.server.s3.testing.builder.StreamObject;

/**
 * JUnit Test rule for running a AWS S3-compatible object storage alongside the (integration) tests.
 * Use {@link #getEndpoint()} to retrieve the endpoint URL to connect to.
 *
 * <p>Example usage:
 *
 * <pre>
 *   &#64;ClassRule
 *   public static final S3MockRule S3_MOCK = S3MockRule.builder().build();
 * </pre>
 *
 * @deprecated Please update to JUnit 5 and use {@link S3ClassExtension}
 */
@Deprecated
public class S3MockRule extends ExternalResource {

  private final S3Mock s3Mock;

  public S3MockRule(List<String> buckets, List<MockObject> mockObjects) {
    s3Mock = new S3Mock(buckets, mockObjects);
  }

  @Override
  protected void before() {
    s3Mock.start();
  }

  @Override
  protected void after() {
    s3Mock.stop();
  }

  public String getEndpoint() {
    return s3Mock.getEndpoint();
  }

  public void resetAll() {
    s3Mock.resetAll();
  }

  public AmazonS3 getClient() {
    return s3Mock.getClient();
  }

  public void start() {
    s3Mock.start();
  }

  public void stop() {
    s3Mock.stop();
  }

  //
  // Builder
  //
  public static Builder builder() {
    return new Builder();
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
    public S3MockRule.Builder createBucket(String bucketName) {
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
    public S3MockRule.Builder putObject(String bucketName, String key, File file) {
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
    public S3MockRule.Builder putObject(String bucketName, String key, String content) {
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
    public S3MockRule.Builder putObject(String bucketName, String key, InputStream stream) {
      createBucket(bucketName);
      mockObjects.add(new StreamObject(bucketName, key, stream));
      return this;
    }

    public S3MockRule build() {
      return new S3MockRule(buckets, mockObjects);
    }
  }
}
