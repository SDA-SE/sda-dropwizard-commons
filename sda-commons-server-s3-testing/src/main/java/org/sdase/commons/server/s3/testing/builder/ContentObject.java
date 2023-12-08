package org.sdase.commons.server.s3.testing.builder;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class ContentObject implements MockObject {
  String bucketName;
  String key;
  String content;

  public ContentObject(String bucketName, String key, String content) {
    this.bucketName = bucketName;
    this.key = key;
    this.content = content;
  }

  public void putObject(S3Client s3Client) {
    s3Client.putObject(
        PutObjectRequest.builder().bucket(bucketName).key(key).build(),
        RequestBody.fromString(content));
  }
}
