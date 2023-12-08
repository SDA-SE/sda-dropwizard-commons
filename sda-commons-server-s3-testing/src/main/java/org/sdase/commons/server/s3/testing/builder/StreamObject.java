package org.sdase.commons.server.s3.testing.builder;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;
import org.apache.commons.io.IOUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/** MockObject that deals with an InputStream. */
public class StreamObject implements MockObject {

  private final String bucketName;
  private final String key;
  private final Supplier<InputStream> streamProvider;

  public StreamObject(String bucketName, String key, Supplier<InputStream> streamProvider) {
    this.bucketName = bucketName;
    this.key = key;
    this.streamProvider = streamProvider;
  }

  @Override
  public void putObject(S3Client s3Client) {
    try (var stream = streamProvider.get()) {
      byte[] bytes = IOUtils.toByteArray(stream);
      s3Client.putObject(
          PutObjectRequest.builder()
              .bucket(bucketName)
              .key(key)
              .contentLength((long) bytes.length)
              .build(),
          RequestBody.fromBytes(bytes));
    } catch (IOException exception) {
      throw new IllegalStateException(exception);
    }
  }

  public String getKey() {
    return key;
  }
}
