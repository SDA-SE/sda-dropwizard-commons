package org.sdase.commons.server.s3.test;

import io.dropwizard.Configuration;
import org.sdase.commons.server.s3.S3Configuration;

public class Config extends Configuration {
  private S3Configuration s3Config = new S3Configuration();

  public S3Configuration getS3Config() {
    return s3Config;
  }

  public void setS3Config(S3Configuration s3Config) {
    this.s3Config = s3Config;
  }
}
