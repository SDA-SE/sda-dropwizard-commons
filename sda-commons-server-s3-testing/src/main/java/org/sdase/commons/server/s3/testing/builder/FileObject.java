package org.sdase.commons.server.s3.testing.builder;

import java.io.File;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class FileObject implements MockObject {
  String bucketName;
  String key;
  File file;

  public FileObject(String bucketName, String key, File file) {
    this.bucketName = bucketName;
    this.key = key;
    this.file = file;
  }

  public void putObject(S3Client s3Client) {
    s3Client.putObject(
        PutObjectRequest.builder().bucket(bucketName).key(key).build(), RequestBody.fromFile(file));
  }

  public String getKey() {
    return key;
  }
}
