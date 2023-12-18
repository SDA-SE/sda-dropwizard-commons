package org.sdase.commons.server.s3.test;

import io.dropwizard.core.Configuration;
import org.sdase.commons.server.s3.S3Configuration;

public class Config extends Configuration {
  private S3Configuration s3Config = new S3Configuration();
  private String s3Bucket;

  public S3Configuration getS3Config() {
    return s3Config;
  }

  public void setS3Config(S3Configuration s3Config) {
    this.s3Config = s3Config;
  }

  public String getS3Bucket() {
    return s3Bucket;
  }

  public Config setS3Bucket(String s3Bucket) {
    this.s3Bucket = s3Bucket;
    return this;
  }
}
