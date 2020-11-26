# SDA Dropwizard Commons Server KMS AWS

[![javadoc](https://javadoc.io/badge2/org.sdase.commons/sda-commons-server-kms-aws/javadoc.svg)](https://javadoc.io/doc/org.sdase.commons/sda-commons-server-kms-aws)

This module provides a [`KmsAwsBundle`](./src/main/java/org/sdase/commons/server/kms/aws/KmsAwsBundle.java) that adds
functionality for encryption and decryption using Customer Master Keys (CMKs) stored within an [AWS Key Management Service](https://aws.amazon.com/de/kms/).

## Related modules

### Kafka

Refer to [`sda-commons-server-kms-aws-kafka`](../sda-commons-server-kms-aws-kafka/README.md) which provides support for encryption and decryption of Kafka messages.

### Testing

[`sda-commons-server-kms-aws-testing`](../sda-commons-server-kms-aws-testing/README.md) provides support for integration testing with AWS KMS and JUnit 4.


## Usage

Add the following dependency:
```groovy
compile 'org.sdase.commons:sda-commons-server-kms-aws'
```

Add the KMS AWS Configuration to your configuration class:
```java

public class YourConfiguration extends SdaPlatformConfiguration {
  // ...
  @NotNull 
  @Valid
  private KmsAwsConfiguration kmsAws = new KmsAwsConfiguration();

  /** Amazon Resource Name (ARN) of the CMK to be used for AWS KMS encryption */
  @NotEmpty
  private String keyArn;

  // ...
}
```
Bootstrap the bundle (see below).

**Customer Master Key (CMK)**

It is the responsibility of the service to provide configuration properties of the KeyArns referencing the CMKS. The CMKs should be referenced by their alias rather than their id to be able to roll the CMK without changing service configuration. 

Also, we advise to use a separate CMK per channel (e.g. topic). 


**Configuration Summary**

The following configuration can be used as base for a production-ready configuration.

```yaml
kmsAws:
  # Can be disabled for testing
  disableAwsEncryption: ${KMS_AWS_DISABLED:-false}

  # Region where the AWS KMS is hosted in.
  region: ${KMS_AWS_REGION:-eu-central-1}

  # The AWS access key.
  accessKeyId: ${KMS_AWS_ACCESS_KEY_ID:-}

  # The AWS secret access key.
  secretAccessKey: ${KMS_AWS_SECRET_ACCESS_KEY:-}

  # Role ARN to be used to start an AWS Client session
  roleArn: ${KMS_AWS_ROLE_ARN:-}

  # Prefix used for identifying the AWS Client session (e.g. service name). It will be appended by a generated UUID for each new session.
  roleSessionNamePrefix: ${KMS_AWS_ROLE_SESSION_NAME_PREFIX:-}

  # For activating key caching the following configuration options are needed. Default: caching is disabled and each data key is only used for encrypting one message
  keyCaching:
    # Key-caching can be enabled
    enabled: ${KMS_AWS_ENABLE_KEY_CACHING:-false}

    # Maximum number of data keys the cache will hold
    maxCacheSize: ${KMS_AWS_MAX_CACHE_SIZE:-0}
    
    # Maximum lifetime of a data key within the cache in seconds before it is removed
    keyMaxLifetimeInSeconds: ${KMS_AWS_CACHE_MAX_LIFETIME_SECONDS:-0}

    # Maximum number of messages that can get encrypted before the data key is not used anymore
    maxMessagesPerKey: ${KMS_AWS_MAX_MESSAGES_PER_CACHED_KEY:-1}
```

A possible documentation could look like this:

```md
## Environment Variables

The following environment variables can be used to configure the Docker container:

// ...

### AWS KMS

* `KMS_AWS_DISABLED`_boolean_
   * Set to `true` to disable encryption/decryption. This value shall not to be set to `true` in production environments.
   * Default: `false`
 
* `KMS_AWS_REGION` _string_
   * Region where the AWS KMS is hosted in.
   * Default: `eu-central-1`

* `KMS_AWS_ACCESS_KEY_ID` _string_
  * The AWS access key.
  * Example: ``

* `KMS_AWS_SECRET_ACCESS_KEY` _string_
  * The AWS secret access key.
  * Example: ``

* `KMS_AWS_ROLE_ARN` _string_
  * Role ARN to be used to start an AWS Client session
  * Example: ``

* `KMS_AWS_ROLE_SESSION_NAME_PREFIX` _string_
  * Prefix used for identifying the AWS Client session. It will be appended by a generated UUID for each new session.
  * Example: `document-ods-service`

For activating key caching the following configuration options are needed. 

Default: caching is disabled and each data key is only used for encrypting a single message

* `KMS_AWS_ENABLE_KEY_CACHING` _boolean_
  * Set to `true` to enable data key caching.
  * Default: `false`

* `KMS_AWS_MAX_CACHE_SIZE` _number_
  * Maximum number of data keys the cache will hold. Set to a value greater than `0` to activate key caching.
  * Default: `0`
    
* `KMS_AWS_CACHE_MAX_LIFETIME_SECONDS` _number_
  * Lifetime of a data key within the cache in seconds. Afterwards it is not used anymore for encryption.
  * Default: `0`

* `KMS_AWS_MAX_MESSAGES_PER_CACHED_KEY` _number_
  * Maximum number of messages that can get encrypted before the data key is not used anymore. Set to a value greater than `0` to re-use a single data key.
  * Default: `1`
```

**Bootstrap**

```java
public class DemoApplication {
   
   private final KmsAwsBundle<MalwareScannerConfiguration> kmsAwsBundle =
         KmsAwsBundle.builder()
             .withConfigurationProvider(AppConfiguration::getKmsAws)
             .build();

   public void initialize(Bootstrap<AppConfiguration> bootstrap) {
      bootstrap.addBundle(kmsAwsBundle);
   }

  public void run(AppConfiguration configuration, Environment environment) {
    KmsAwsEncryptionService kmsAwsEncryptionService = 
        awsKmsBundle.createKmsAwsEncryptionService(configuration.getKeyArn());
    KmsAwsDecryptionService kmsAwsDecryptionService = createKmsAwsDecryptionService();
    DemoManager manager = new DemoManger(kmsAwsEncryptionService, kmsAwsDecryptionService);
   }
}

public class DemoManager {

   private final KmsAwsEncryptionService kmsAwsEncryptionService;
   private final KmsAwsDecryptionService kmsAwsDecryptionService;

   public DemoManager(
      KmsAwsEncryptionService kmsAwsEncryptionService, 
      KmsAwsDecryptionService kmsAwsDecryptionService) {

       this.kmsAwsEncryptionService = kmsAwsEncryptionService;
       this.kmsAwsDecryptionService = kmsAwsDecryptionService;
   }

   public void doSomeEncryption() {
      byte[] plainTextBytes = "hello World".getBytes();

      Map<String, String> encryptionContext = new HashMap<>();
      encryptionContext.put("key1", "unconfidential-value");

      byte[] encryptedData = kmsAwsEncryptionService.encrypt(plainTextBytes, encryptionContext);
   }
   
   public void doSomeDecryption(byte[] ciphertext) {
      Map<String, String> expectedEncryptionContext = new HashMap<>();
      encryptionContext.put("key1", "unconfidential-value");

      try {
         byte[] plainTextBytes = kmsAwsDecryptionService.decrypt(ciphertext, expectedEncryptionContext);
      } catch (InvalidEncryptionContextExcpetion e) {
          // handle exception
      }
   }
}
```

**Encryption Context**

The EncryptionContext is an implementation of Additional authenticated data (AAD). It ensures that unencrypted data related to the ciphertext is protected against tampering. Data that is commonly used for AAD might include header information, unencrypted database fields in the same record, file names, or other metadata.  It is important to remember that the encryption context should contain only *nonsensitive* information as it is not being encrypted and can be seen in other AWS products (e.g. AWS CloudTrail) in plaintext.

The encryption context is a key-value map (both Strings) which is provided to KMS with each encryption/decryption operation. The entries of the encryption context used during encryption must match the entries during decryption or an `org.sdase.commons.server.kms.aws.InvalidEncryptionContextException` is thrown.

For more information on the Encryption Context read the [AWS Security Blog](https://aws.amazon.com/de/blogs/security/how-to-protect-the-integrity-of-your-encrypted-data-by-using-aws-key-management-service-and-encryptioncontext/).