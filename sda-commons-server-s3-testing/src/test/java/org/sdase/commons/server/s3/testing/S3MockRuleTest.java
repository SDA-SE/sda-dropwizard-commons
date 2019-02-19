package org.sdase.commons.server.s3.testing;

import static org.assertj.core.api.Assertions.assertThat;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

public class S3MockRuleTest {

   @ClassRule
   public static final S3MockRule S3_MOCK = S3MockRule.builder().build();

   private AmazonS3 s3Client;

   @Before
   public void setUp() {
      // The S3 Mock doesn't require authentication, however we still pass it
      // here to check that the
      // server is at least ignoring it
      AWSCredentials credentials = new BasicAWSCredentials("user", "s3cr3t");
      ClientConfiguration clientConfiguration = new ClientConfiguration();
      clientConfiguration.setSignerOverride("AWSS3V4SignerType");

      s3Client = AmazonS3ClientBuilder
            .standard()
            .withEndpointConfiguration(
                  new AwsClientBuilder.EndpointConfiguration(S3_MOCK.getEndpoint(),
                      Regions.DEFAULT_REGION.getName()))
            .withPathStyleAccessEnabled(true)
            .withClientConfiguration(clientConfiguration)
            .withCredentials(new AWSStaticCredentialsProvider(credentials))
            .build();
   }

   @Test()
   public void shouldStartS3Service() {
      String bucketName = "text-bucket";
      String fileName = "test-file.txt";
      String fileContent = "Hallo Welt!";

      s3Client.createBucket(bucketName);
      s3Client.putObject(bucketName, fileName, fileContent);
      S3Object object = s3Client.getObject(bucketName, fileName);
      assertThat(object.getObjectContent()).hasContent(fileContent);
   }
}