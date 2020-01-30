package org.sdase.commons.server.s3.testing.builder;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.util.IOUtils;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

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
  public void putObject(AmazonS3 s3Client) {
    try {
      byte[] bytes = IOUtils.toByteArray(stream);

      try (ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes)) {
        s3Client.putObject(bucketName, key, byteStream, new ObjectMetadata());
      }
    } catch (IOException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
