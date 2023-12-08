package org.sdase.commons.server.s3.health;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.annotation.Async;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

@Async(period = 30, scheduleType = Async.ScheduleType.FIXED_DELAY)
public class S3HealthCheck extends HealthCheck {

  private static final Logger LOG = LoggerFactory.getLogger(S3HealthCheck.class);

  private final S3Client s3Client;
  private final Set<String> bucketNames;

  public S3HealthCheck(S3Client s3Client, Set<String> bucketNames) {
    this.s3Client = s3Client;
    this.bucketNames = bucketNames;
  }

  @Override
  protected Result check() {
    try {
      return bucketNames.stream()
              .map(name -> HeadBucketRequest.builder().bucket(name).build())
              .allMatch(
                  headBucketRequest -> {
                    try {
                      var response = s3Client.headBucket(headBucketRequest);
                      return response.sdkHttpResponse().isSuccessful();
                    } catch (NoSuchBucketException e) {
                      LOG.info("Bucket {} does not exist.", headBucketRequest.bucket());
                      return false;
                    }
                  })
          ? Result.healthy()
          : Result.unhealthy("One or more buckets don't exist exist.");
    } catch (Exception ex) {
      LOG.warn("S3 health check failed!", ex);
      return Result.unhealthy("Could not establish an S3 connection.");
    }
  }
}
