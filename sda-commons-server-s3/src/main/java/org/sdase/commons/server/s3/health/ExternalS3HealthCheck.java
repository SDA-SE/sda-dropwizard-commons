package org.sdase.commons.server.s3.health;

import java.util.Set;
import org.sdase.commons.server.healthcheck.ExternalHealthCheck;
import software.amazon.awssdk.services.s3.S3Client;

@ExternalHealthCheck
public class ExternalS3HealthCheck extends S3HealthCheck {
  public ExternalS3HealthCheck(S3Client s3Client, Set<String> bucketName) {
    super(s3Client, bucketName);
  }
}
