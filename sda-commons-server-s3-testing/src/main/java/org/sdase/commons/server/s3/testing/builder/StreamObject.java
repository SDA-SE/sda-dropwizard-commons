package org.sdase.commons.server.s3.testing.builder;

import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class StreamObject implements MockObject {

  private final String bucketName;
  private final String key;
  private final InputStream stream;

  public StreamObject(String bucketName, String key, InputStream stream) {
    this.bucketName = bucketName;
    this.key = key;
    this.stream = stream;
  }

  @Override
  public void putObject(S3Client s3Client) {
    try {
      byte[] bytes = IOUtils.toByteArray(stream);
      s3Client.putObject(
          PutObjectRequest.builder().bucket(bucketName).key(key).build(),
          RequestBody.fromBytes(bytes));
    } catch (IOException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
