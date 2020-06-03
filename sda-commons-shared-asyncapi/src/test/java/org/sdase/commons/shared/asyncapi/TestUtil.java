package org.sdase.commons.shared.asyncapi;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TestUtil {

  private TestUtil() {
    // No public constructor
  }

  static String readResource(String filename) throws URISyntaxException, IOException {
    return new String(Files.readAllBytes(Paths.get(TestUtil.class.getResource(filename).toURI())));
  }
}
