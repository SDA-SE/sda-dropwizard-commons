package org.sdase.commons.server.s3.testing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import org.sdase.commons.server.s3.testing.builder.MockObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;

public class S3Manager {

  private static final Logger LOG = LoggerFactory.getLogger(S3Manager.class);

  /**
   * Deletes all buckets from the S3 service. It reads all existing buckets and deletes them one by
   * one. If a bucket is not empty, it will be emptied before deletion.
   *
   * @param client s3 client
   */
  void deleteAllBuckets(S3Client client) {
    var listBucketResponse = client.listBuckets();
    listBucketResponse.buckets().forEach(bucket -> deleteBucket(client, bucket.name()));
  }

  /**
   * Deletes a bucket from the S3 service. If the bucket is not empty, it will be emptied before
   * deletion.
   *
   * @param client s3 client
   * @param bucketName name of the bucket to delete
   */
  void deleteBucket(S3Client client, String bucketName) {
    LOG.info("Delete bucket: {}", bucketName);
    deleteObjectsFromBucket(client, bucketName);
    var response = client.deleteBucket(DeleteBucketRequest.builder().bucket(bucketName).build());
    if (!response.sdkHttpResponse().isSuccessful()) {
      LOG.info("Could not delete bucket: {}", bucketName);
    }
  }

  /**
   * Deletes all objects from a bucket.
   *
   * @param client s3 client
   * @param bucketName name of the bucket to delete
   */
  void deleteObjectsFromBucket(S3Client client, String bucketName) {
    var response = client.listObjects(ListObjectsRequest.builder().bucket(bucketName).build());
    response.contents().stream()
        .map(
            s3Object ->
                DeleteObjectRequest.builder().bucket(bucketName).key(s3Object.key()).build())
        .forEach(client::deleteObject);
    if (!response.sdkHttpResponse().isSuccessful()) {
      LOG.info("Could not delete objects in bucket: {}", bucketName);
    }
  }

  /**
   * Creates buckets in the S3 service.
   *
   * @param client s3 client
   * @param buckets names of the buckets to create
   */
  void createBuckets(S3Client client, Collection<String> buckets) {
    buckets.stream()
        .filter(b -> b != null && !b.isBlank())
        .forEach(
            bucketName -> {
              LOG.info("Create bucket: {}", bucketName);
              var response =
                  client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
              assertThat(response.sdkHttpResponse().isSuccessful()).isTrue();
            });
  }

  /**
   * Creates mock objects in the S3 service.
   *
   * @param client s3 client
   * @param mockObjects mock objects to create
   */
  void createMockObjects(S3Client client, Collection<MockObject> mockObjects) {
    mockObjects.forEach(
        mockObject -> {
          LOG.info("Create mock object: {}", mockObject.getKey());
          mockObject.putObject(client);
        });
  }
}
