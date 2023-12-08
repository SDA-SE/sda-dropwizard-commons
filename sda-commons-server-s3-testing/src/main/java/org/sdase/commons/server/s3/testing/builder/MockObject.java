package org.sdase.commons.server.s3.testing.builder;

import software.amazon.awssdk.services.s3.S3Client;

public interface MockObject {

  String getKey();

  void putObject(S3Client s3Client);
}
