package org.sdase.commons.shared.certificates.ca.ssl;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CertificateReader {
  private static final Logger LOG = LoggerFactory.getLogger(CertificateReader.class);
  private static final int MAX_RECURSIVE_DEPTH = 10;

  private final String pathToCertificatesDir;

  public CertificateReader(String pathToCertificatesDir) {
    this.pathToCertificatesDir = pathToCertificatesDir;
  }

  public Optional<String> readCertificates() {
    Optional<Path> optionalPath = toPathIfExists(pathToCertificatesDir);
    if (optionalPath.isPresent()) {
      LOG.info("Checking for CA certificates in {}", pathToCertificatesDir);
      return findAllPemContent(optionalPath.get());
    } else {
      LOG.info(
          "Not collecting CA certificates, {} does not exist or is not a directory.",
          pathToCertificatesDir);
      return Optional.empty();
    }
  }

  private Optional<Path> toPathIfExists(String location) {

    Optional<Path> path =
        Optional.ofNullable(Paths.get(location)).filter(Files::exists).filter(Files::isDirectory);
    if (path.isPresent() && !Files.isReadable(path.get())) {
      throw new IllegalStateException(
          String.format("Existing directory %s is not readable.", location));
    }
    return path;
  }

  private Optional<String> findAllPemContent(Path path) {
    try (Stream<Path> pathStream = Files.walk(path, MAX_RECURSIVE_DEPTH)) {
      return Optional.of(
              pathStream
                  .peek(p -> LOG.debug("Checking {}", p)) // NOSONAR java:S3864
                  .filter(Files::isRegularFile)
                  .filter(this::isPemFile)
                  .map(this::readContent)
                  .collect(Collectors.joining("\n\n")))
          .filter(StringUtils::isNotBlank);
    } catch (IOException e) {
      throw new UncheckedIOException(String.format("Failed to read pem files from %s", path), e);
    }
  }

  private boolean isPemFile(Path filePath) {
    boolean isPemFile = filePath.getFileName().toString().endsWith(".pem");
    if (!isPemFile) {
      LOG.info("Omitting {}: not a .pem file", filePath);
    }
    return isPemFile;
  }

  private String readContent(Path pemFile) {
    try {
      return new String(Files.readAllBytes(pemFile), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(String.format("Failed to read %s", pemFile), e);
    }
  }
}
