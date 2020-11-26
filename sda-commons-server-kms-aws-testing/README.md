# SDA Commons Server Kafka Testing

[![javadoc](https://javadoc.io/badge2/org.sdase.commons/sda-commons-server-kms-aws-testing/javadoc.svg)](https://javadoc.io/doc/org.sdase.commons/sda-commons-server-kmw-aws-testing)

The module `sda-commons-server-aws-kmw-testing` is the base module to add unit and integrations 
test for AWS KMS.

## Usage

You can add `KmsAwsRule` as JUnit 4 class rule to your test setup and use it as your
MKS endpoint by overriding configuration for `kmsAws.endpointUrl` 

```
@ClassRule
public static final KmsAwsRule kmsAwsRule = new KmsAwsRule();

new DropwizardAppRule<>(
            ExampleApplication.class,
            resourceFilePath("test-config.yml"),
            config("kmsAws.endpointUrl", kmsAwsRule::getEndpointUrl)); // Override the KMS endpoint
```