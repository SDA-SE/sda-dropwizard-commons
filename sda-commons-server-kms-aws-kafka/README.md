# SDA Dropwizard Commons Server AWS KMS Kafka

[![javadoc](https://javadoc.io/badge2/org.sdase.commons/sda-commons-server-kms-aws-kafka/javadoc.svg)](https://javadoc.io/doc/org.sdase.commons/sda-commons-server-kms-aws-kafka)

This module extends the [`sda-commons-server-kms-aws`](../sda-commons-server-kms-aws/README.md) module by providing a [`KmsAwsKafkaBundle`](./src/main/java/org/sdase/commons/server/kms/aws/kafka/KmsAwsKafkaBundle.java). This bundle adds convenience methods for wrapping Serializers and Deserializers for encryption and decryption respectively.

Refer to [`sda-commons-server-kms-aws`](../sda-commons-server-kms-aws/README.md) for encryption/decryption capabilities without Kafka support.


## Usage

Add the following dependency:
```groovy
compile 'org.sdase.commons:sda-commons-server-kms-aws-kafka'
```
This dependency automatically pulls in the dependencies
 * `org.sdase.commons:sda-commons-server-kafka`
 * `org.sdase.commons:sda-commons-server-kms-aws`

**Configuration Summary**

For configuration details refer to [`sda-commons-server-kms-aws`](../sda-commons-server-kms-aws/README.md).

**Bootstrap**

The `KmsAwsKafkaBundle` should be added as a field to the application since it provides methods for wrapping Serializers and Deserializers.

```java
public class DemoApplication {

   private final KafkaBundle<AppConfiguration> kafkaBundle = 
      KafkaBundle.builder()
          .withConfigurationProvider(AppConfiguration::getKafka)
          .build();
   
   // add KmsAwsBundleKafka
   private final KmsAwsKafkaBundle<MalwareScannerConfiguration> kmsAwsKafkaBundle =
       KmsAwsKafkaBundle.builder()
          .withConfigurationProvider(AppConfiguration::getKmsAws)
          .build();

   public void initialize(Bootstrap<AppConfiguration> bootstrap) {
      bootstrap.addBundle(kafkaBundle);
      bootstrap.addBundle(kmsAwsBundle);
   }

   public void run(AppConfiguration configuration, Environment environment) {
      // Only needed for allowing of encryption/decryption of Kafka messages. 
      // Existing Kafka initialization stays as is, but the ValueSerializers/ValueDeserializers 
      // are wrapped by the respective Serializer/Deserializer of the KmsAwsBundle.

      MessageHandler<String, String> handler = record -> results.add(record.value());

      // no need to reference a CMK, as it will be derived from the encrypted message
      AwsEncryptionSerializer<T> wrappedValueDeserializer = 
         kmsAwsKafkaBundle.wrapDeserializer(new StringDeserializer());

      kafkaBundle.createMessageListener(MessageListenerRegistration.<String, String> builder()
         .withDefaultListenerConfig()
         .forTopic(topic) 
         .withDefaultConsumer()
         .withValueDeserializer(wrappedValueDeserializer) // just wrap the Deserializer
         .withListenerStrategy(new AutocommitMLS<String, String>(handler, handler))
         .build());
    
      // use separate CMK per topic
      String keyArn = configuration.getKeyArnForTopic();
      AwsEncryptionSerializer<T> wrappedValueSerializer = 
               kmsAwsKafkaBundle.wrapSerializer(new KafkaJsonSerializer<>(new ObjectMapper()), keyArn);

      MessageProducer<String, SimpleEntity> jsonProducer = kafkaBundle.registerProducer(ProducerRegistration
         .builder()
         .forTopic(topic)
         .withDefaultProducer()
         .withKeySerializer(new StringSerializer())
         .withValueSerializer(wrappedValueSerializer).build());
   }   
}
```
