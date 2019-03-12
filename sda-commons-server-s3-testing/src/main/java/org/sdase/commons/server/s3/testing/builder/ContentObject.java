package org.sdase.commons.server.s3.testing.builder;

import com.amazonaws.services.s3.AmazonS3;

public class ContentObject implements MockObject {
   String bucketName;
   String key;
   String content;

   public ContentObject(String bucketName, String key, String content) {
      this.bucketName = bucketName;
      this.key = key;
      this.content = content;
   }

   public void putObject(AmazonS3 s3Client) {
      s3Client.putObject(bucketName, key, content);
   }
}