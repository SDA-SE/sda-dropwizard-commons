package org.sdase.commons.server.s3.testing;

import static io.dropwizard.testing.ResourceHelpers.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import java.io.File;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

public class S3MockRuleTest {
  private static final String PRE_FILLED_BUCKET = "pre-filled-bucket";
  private static final String WATER_BUCKET = "bucket-of-water";

  @ClassRule
  public static final S3MockRule S3_MOCK =
      S3MockRule.builder()
          .createBucket(WATER_BUCKET)
          .putObject(PRE_FILLED_BUCKET, "file.txt", new File(resourceFilePath("test-file.txt")))
          .putObject(
              PRE_FILLED_BUCKET,
              "stream.txt",
              S3MockRuleTest.class.getResourceAsStream("/test-file.txt"))
          .putObject(PRE_FILLED_BUCKET, "content.txt", "RUN SDA")
          .build();

  private AmazonS3 s3Client;

  @Before
  public void setUp() {
    // The S3 Mock doesn't require authentication, however we still pass it
    // here to check that the server is at least ignoring it
    AWSCredentials credentials = new BasicAWSCredentials("user", "s3cr3t");
    ClientConfiguration clientConfiguration = new ClientConfiguration();
    clientConfiguration.setSignerOverride("AWSS3V4SignerType");

    s3Client = S3_MOCK.getClient();
  }

  @Test()
  public void shouldStartS3Service() {
    String bucketName = "test-bucket";
    String fileName = "test-file.txt";
    String fileContent = "Hallo Welt!";

    s3Client.createBucket(bucketName);
    s3Client.putObject(bucketName, fileName, fileContent);
    S3Object object = s3Client.getObject(bucketName, fileName);
    assertThat(object.getObjectContent()).hasContent(fileContent);
  }

  @Test()
  public void shouldExistBucket() {
    boolean exists = s3Client.doesBucketExistV2(WATER_BUCKET);
    assertThat(exists).isTrue();
  }

  @Test()
  public void shouldExistPreCreatedFromFile() {
    boolean exists = s3Client.doesObjectExist(PRE_FILLED_BUCKET, "file.txt");
    assertThat(exists).isTrue();
  }

  @Test()
  public void shouldExistPreCreatedFromInputStream() {
    S3Object object = s3Client.getObject(PRE_FILLED_BUCKET, "content.txt");
    assertThat(object.getObjectContent()).hasContent("RUN SDA");
  }

  @Test()
  public void shouldExistPreCreatedFromString() {
    S3Object object = s3Client.getObject(PRE_FILLED_BUCKET, "content.txt");
    assertThat(object.getObjectContent()).hasContent("RUN SDA");
  }

  @Test()
  public void shouldResetAll() {
    s3Client.createBucket("new");
    s3Client.putObject("new", "data", "to be deleted");

    S3_MOCK.resetAll();

    boolean objectExists = s3Client.doesObjectExist("new", "data");
    assertThat(objectExists).isFalse();
    boolean bucketExists = s3Client.doesBucketExistV2(WATER_BUCKET);
    assertThat(bucketExists).isTrue();
  }
}
