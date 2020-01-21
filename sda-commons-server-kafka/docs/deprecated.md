# server-kafka

## Deprecated APIs

The bundle still supports the deprecated `KafkaBundle::registerMessageHandler` to support backward compatibility. In this context some additional 
but also deprecated listener config properties are available. The `MessageListener` requires the implementation of the `MessageHandler` interface. The Builder
for `MessageHandlerRegistration` supports the user in the creation of these complex configurable objects. 

The former logic is implemented in a [`LegacyMLS`](../../sda-commons-server-kafka/src/main/java/org/sdase/commons/server/kafka/consumer/strategies/legacy/LegacyMLS.java):
The user can choose between auto-commit, sync, and async commits by configuration. But, the `MessageListener` does not implement
any extra logic in case of rebalancing. Therefore, the listener does not support an exactly once semantic. It might occur
that messages are redelivered after rebalance activities.
 
```
public class DemoApplication {
   private final KafkaBundle<AppConfiguration> kafkaBundle = KafkaBundle.builder().withConfigurationProvider(AppConfiguration::getKafka).build();
   private final MessageProducer<String, ProductBundle> producer;
      
   public void initialize(Bootstrap<AppConfiguration> bootstrap) {
      bootstrap.addBundle(kafkaBundle);
   }
         
   public void run(AppConfiguration configuration, Environment environment) throws Exception {
      // register with default consumer and listener config
      // The handler implements the actual logic for the message processing
      
      kafkaBundle
            .registerMessageHandler(MessageHandlerRegistration
                  .<String, String> builder()
                  .withDefaultListenerConfig()
                  .forTopic("topic") // replace topic with your topic name
                  .withDefaultConsumer()
                  .withHandler(record -> results.add(record.value())) // replace with your handler implementation
                  .withErrorHandler(new IgnoreAndProceedErrorHandler<>())
                  .build());
                         
      // register with custom consumer and listener configuration (e.g. 2 instances, poll every minute)
      // method returns list of listeners, one for each instance
      List<MessageListener> listener = kafkaBundle
            .registerMessageHandler(MessageHandlerRegistration
                  .builder()
                  .withListenerConfig(ListenerConfig.builder().withPollInterval(60000).build(2))
                  .forTopic("topic") // replace topic with your topic name
                  .withConsumerConfig("consumer2") // use consumer config from config yaml
                  .withHandler(x -> result.add(x)) // replace with your handler implementation
                  .withErrorHandler(new IgnoreAndProceedErrorHandler<>())
                  .build());
```

### Configuration
To configure KafkaBundle add the following `kafka` block to your Dropwizard config.yml. The following config snippet shows an example configuration with descriptive comments:
```YAML
kafka:
  ...
  
  # Map with listener configurations that can be used within MessageListener creation.
  listenerConfig:
    # id/name of the listener configuration
    async:
      # Defines the commit type used when using explicit commit instead of auto commit.
      # ASYNC: Async commit is used and CallbackMessageHandler can be used to implement logic when the commit returns
      # SYNC: (Default) Sync commit uses the synchronous commit call. Exception is thrown when commit fails. 
      commitType: ASYNC
      # Defines if explicit commit should be used. If false, explicit commit is used. Default: true
      useAutoCommitOnly: false
      # Number of listener instances that will be generated. If > 1, several KafkaConsumer are generated. Kafka assigns these consumers
      # to different partitions of the consumed topic. Number instances should be smaller or equal to the number of partitions.  
      instances: 1
      # If the topic check is configured within the DSL, the listener waits this amount of ms before checking topic existence again. 0 will disable existence check even when configured in DSL
      topicMissingRetryMs: 60000
```

#### listenerConfig
| Key | Value |
|-----|-------|
| instances | 1 |
| commitType | SYNC |
| useAutoCommitOnly | true |
| topicMissingRetryMs | 0 |