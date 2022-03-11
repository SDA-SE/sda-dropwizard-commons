package org.sdase.commons.server.s3.health;

import com.amazonaws.services.s3.AmazonS3;
import java.util.Set;
import org.sdase.commons.server.healthcheck.ExternalHealthCheck;

@ExternalHealthCheck
public class ExternalS3HealthCheck extends S3HealthCheck {
  public ExternalS3HealthCheck(AmazonS3 s3Client, Set<String> bucketName) {
    super(s3Client, bucketName);
  }
}
