package org.sdase.commons.server.s3.testing;

import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.assertj.core.api.Assertions.assertThat;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import java.io.File;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class S3ClassExtensionTest {
  private static final String PRE_FILLED_BUCKET = "pre-filled-bucket";
  private static final String WATER_BUCKET = "bucket-of-water";

  @RegisterExtension
  static final S3ClassExtension S3_EXTENSION =
      S3ClassExtension.builder()
          .createBucket(WATER_BUCKET)
          .putObject(PRE_FILLED_BUCKET, "file.txt", new File(resourceFilePath("test-file.txt")))
          .putObject(
              PRE_FILLED_BUCKET,
              "stream.txt",
              S3MockRuleTest.class.getResourceAsStream("/test-file.txt"))
          .putObject(PRE_FILLED_BUCKET, "content.txt", "RUN SDA")
          .build();

  private AmazonS3 s3Client;

  @BeforeEach
  void setUp() {
    // The S3 Mock doesn't require authentication, however we still pass it
    // here to check that the server is at least ignoring it
    AWSCredentials credentials = new BasicAWSCredentials("user", "s3cr3t");
    ClientConfiguration clientConfiguration = new ClientConfiguration();
    clientConfiguration.setSignerOverride("AWSS3V4SignerType");

    s3Client = S3_EXTENSION.getClient();
  }

  @Test
  void shouldStartS3Service() {
    String bucketName = "test-bucket";
    String fileName = "test-file.txt";
    String fileContent = "Hallo Welt!";

    s3Client.createBucket(bucketName);
    s3Client.putObject(bucketName, fileName, fileContent);
    S3Object object = s3Client.getObject(bucketName, fileName);
    assertThat(object.getObjectContent()).hasContent(fileContent);
  }

  @Test
  void shouldExistBucket() {
    boolean exists = s3Client.doesBucketExistV2(WATER_BUCKET);
    assertThat(exists).isTrue();
  }

  @Test
  void shouldExistPreCreatedFromFile() {
    boolean exists = s3Client.doesObjectExist(PRE_FILLED_BUCKET, "file.txt");
    assertThat(exists).isTrue();
  }

  @Test
  void shouldExistPreCreatedFromInputStream() {
    S3Object object = s3Client.getObject(PRE_FILLED_BUCKET, "content.txt");
    assertThat(object.getObjectContent()).hasContent("RUN SDA");
  }

  @Test
  void shouldExistPreCreatedFromString() {
    S3Object object = s3Client.getObject(PRE_FILLED_BUCKET, "content.txt");
    assertThat(object.getObjectContent()).hasContent("RUN SDA");
  }

  @Test
  void shouldResetAll() {
    s3Client.createBucket("new");
    s3Client.putObject("new", "data", "to be deleted");

    S3_EXTENSION.resetAll();

    boolean objectExists = s3Client.doesObjectExist("new", "data");
    assertThat(objectExists).isFalse();
    boolean bucketExists = s3Client.doesBucketExistV2(WATER_BUCKET);
    assertThat(bucketExists).isTrue();
  }
}
