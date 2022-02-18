package org.sdase.commons.server.s3.health;

import com.amazonaws.services.s3.AmazonS3;
import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.annotation.Async;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Async(period = 30, scheduleType = Async.ScheduleType.FIXED_DELAY)
public class S3HealthCheck extends HealthCheck {

  private static final Logger LOG = LoggerFactory.getLogger(S3HealthCheck.class);

  private final AmazonS3 s3Client;
  private final String[] bucketNames;

  public S3HealthCheck(AmazonS3 s3Client, String[] bucketNames) {
    this.s3Client = s3Client;
    this.bucketNames = bucketNames;
  }

  @Override
  protected Result check() {
    try {
      return Arrays.stream(bucketNames).allMatch(s3Client::doesBucketExistV2)
          ? Result.healthy()
          : Result.unhealthy("One or more buckets are not existing.");
    } catch (Exception ex) {
      LOG.warn("S3 health check failed!", ex);
      return Result.unhealthy("Could not establish an S3 connection.");
    }
  }
}
