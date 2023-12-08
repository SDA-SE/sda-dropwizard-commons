package org.sdase.commons.server.s3.testing;

import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.robothy.s3.jupiter.LocalS3;
import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@LocalS3
class S3ClassExtensionTest {
  private static final String PRE_FILLED_BUCKET = "pre-filled-bucket";
  private static final String WATER_BUCKET = "bucket-of-water";

  @RegisterExtension
  static final S3ClassExtension S3 =
      S3ClassExtension.builder()
          .createBucket(WATER_BUCKET)
          .putObject(PRE_FILLED_BUCKET, "file.txt", new File(resourceFilePath("test-file.txt")))
          .putObject(
              PRE_FILLED_BUCKET,
              "stream.txt",
              () -> S3ClassExtension.class.getResourceAsStream("/test-file.txt"))
          .putObject(PRE_FILLED_BUCKET, "content.txt", "RUN SDA")
          .build();

  private S3Client s3Client;

  @BeforeEach
  void setUp() {
    s3Client = S3.newClient();
    S3.resetAll();
  }

  @Test
  void shouldStartS3Service() throws IOException {
    String bucketName = "test-bucket";
    String fileName = "test-file.txt";
    String fileContent = "Hallo Welt!";

    s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
    s3Client.putObject(
        PutObjectRequest.builder().bucket(bucketName).key(fileName).build(),
        RequestBody.fromString(fileContent));
    var object =
        s3Client.getObject(GetObjectRequest.builder().bucket(bucketName).key(fileName).build());
    assertThat(object.readAllBytes()).asString().isEqualTo(fileContent);
  }

  @Test
  void shouldExistBucket() {
    var response = s3Client.headBucket(HeadBucketRequest.builder().bucket(WATER_BUCKET).build());
    assertThat(response.sdkHttpResponse().isSuccessful()).isTrue();
  }

  @Test
  void shouldExistPreCreatedFromFile() {
    var response =
        s3Client.headObject(
            HeadObjectRequest.builder().bucket(PRE_FILLED_BUCKET).key("file.txt").build());
    assertThat(response.sdkHttpResponse().isSuccessful()).isTrue();
  }

  @Test
  void shouldExistPreCreatedFromInputStream() throws IOException {
    var result =
        s3Client.getObject(
            GetObjectRequest.builder().bucket(PRE_FILLED_BUCKET).key("stream.txt").build());
    assertThat(result.readAllBytes()).asString().isEqualTo("Lorem Ipsum");
  }

  @Test
  void shouldExistPreCreatedFromString() throws IOException {
    var result =
        s3Client.getObject(
            GetObjectRequest.builder().bucket(PRE_FILLED_BUCKET).key("content.txt").build());
    assertThat(result.readAllBytes()).asString().isEqualTo("RUN SDA");
  }

  @Test
  void shouldResetAll() {
    s3Client.createBucket(CreateBucketRequest.builder().bucket("new").build());
    s3Client.putObject(
        PutObjectRequest.builder().bucket("new").key("data").build(),
        RequestBody.fromString("to be deleted"));

    S3.resetAll();

    HeadObjectRequest headObjectRequest =
        HeadObjectRequest.builder().bucket("new").key("data").build();
    assertThatCode(() -> s3Client.headObject(headObjectRequest))
        .isInstanceOf(NoSuchKeyException.class);

    var waterBucketExistsResponse =
        s3Client.headBucket(HeadBucketRequest.builder().bucket(WATER_BUCKET).build());
    assertThat(waterBucketExistsResponse.sdkHttpResponse().isSuccessful()).isTrue();
  }
}
