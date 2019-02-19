package org.sdase.commons.server.s3.testing.builder;

import com.amazonaws.services.s3.AmazonS3;
import java.io.File;

public class FileObject {
   String bucketName;
   String key;
   File file;

   public FileObject(String bucketName, String key, File file) {
      this.bucketName = bucketName;
      this.key = key;
      this.file = file;
   }

   public void putObject(AmazonS3 s3Client) {
      s3Client.putObject(bucketName, key, file);
   }
}
