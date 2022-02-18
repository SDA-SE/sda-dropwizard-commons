package org.sdase.commons.server.s3.testing;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;
import org.sdase.commons.server.s3.testing.builder.MockObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class S3Mock {

  private static final Logger LOGGER = LoggerFactory.getLogger(S3Mock.class);

  private final List<String> buckets;
  private final List<MockObject> mockObjects;

  private io.findify.s3mock.S3Mock internalS3Mock;
  private Integer port;

  public S3Mock(final List<String> buckets, final List<MockObject> mockObjects) {
    this.buckets = buckets;
    this.mockObjects = mockObjects;
  }

  protected void start() {
    port = getFreePort();
    internalS3Mock =
        new io.findify.s3mock.S3Mock.Builder().withInMemoryBackend().withPort(port).build();
    internalS3Mock.start();
    LOGGER.info("Started S3 Mock at {}", getEndpoint());

    AmazonS3 s3Client = getClient();
    initializeObjects(s3Client);
  }

  protected void stop() {
    internalS3Mock.stop();
    LOGGER.info("Stopped S3 Mock");
  }

  protected void shutdown() {
    internalS3Mock.shutdown();
    LOGGER.info("Shutted down S3 Mock");
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
      throw new IllegalStateException("S3Mock not started yet, port not available");
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
}
