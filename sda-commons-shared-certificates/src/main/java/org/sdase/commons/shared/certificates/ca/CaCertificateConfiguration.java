package org.sdase.commons.shared.certificates.ca;

public class CaCertificateConfiguration {
  /**
   * The directory that a CA certificate (list) in PEM format. This certificates are added to the
   * {@link javax.net.ssl.TrustManager}s to verify the connection.
   */
  private String customCaCertificateDir;

  public String getCustomCaCertificateDir() {
    return customCaCertificateDir;
  }

  public void setCustomCaCertificateDir(String customCaCertificateDir) {
    this.customCaCertificateDir = customCaCertificateDir;
  }
}
