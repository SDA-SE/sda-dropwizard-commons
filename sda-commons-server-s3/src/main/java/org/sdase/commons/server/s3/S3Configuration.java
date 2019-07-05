package org.sdase.commons.server.s3;

import org.hibernate.validator.constraints.NotEmpty;

/**
 * Defines the configuration ot the {@link S3Bundle}.
 */
public class S3Configuration {

   /**
    * <p>
    * URL of an S3-compatible endpoint.
    * </p>
    * 
    * <pre>
    * {@code http://servername:8080}
    * </pre>
    */
   @NotEmpty
   private String endpoint;

   /**
    * <p>
    * Region where the S3 storage is located, can be left empty.
    * </p>
    */
   private String region = "";

   /**
    * <p>
    * The access key to identify the accessor.
    * </p>
    */
   @NotEmpty
   private String accessKey;

   /**
    * <p>
    * The secret key of the accessor.
    * </p>
    */
   @NotEmpty
   private String secretKey;

   /**
    * <p>
    * The signer type to use, overrides the default behavior, default is
    * {@code AWSS3V4SignerType}.
    * See documentation of {@code com.amazonaws.ClientConfiguration.setSignerOverride} for more details
    * </p>
    */
   private String signerOverride = "AWSS3V4SignerType";

   public String getEndpoint() {
      return endpoint;
   }

   public S3Configuration setEndpoint(String endpoint) {
      this.endpoint = endpoint;
      return this;
   }

   public String getRegion() {
      return region;
   }

   public S3Configuration setRegion(String region) {
      this.region = region;
      return this;
   }

   public String getAccessKey() {
      return accessKey;
   }

   public S3Configuration setAccessKey(String accessKey) {
      this.accessKey = accessKey;
      return this;
   }

   public String getSecretKey() {
      return secretKey;
   }

   public S3Configuration setSecretKey(String secretKey) {
      this.secretKey = secretKey;
      return this;
   }

   public String getSignerOverride() {
      return signerOverride;
   }

   public S3Configuration setSignerOverride(String signerOverride) {
      this.signerOverride = signerOverride;
      return this;
   }
}
