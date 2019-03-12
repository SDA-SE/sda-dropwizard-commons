package org.sdase.commons.server.s3.testing.builder;

import com.amazonaws.services.s3.AmazonS3;

public interface MockObject {

  void putObject(AmazonS3 s3Client);
}
