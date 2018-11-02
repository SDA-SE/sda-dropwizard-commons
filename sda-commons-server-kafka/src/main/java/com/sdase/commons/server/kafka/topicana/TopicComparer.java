package com.sdase.commons.server.kafka.topicana;

import com.github.ftrossbach.club_topicana.core.ComparisonResult;
import com.github.ftrossbach.club_topicana.core.EvaluationException;
import com.github.ftrossbach.club_topicana.core.ExpectedTopicConfiguration;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.config.ConfigResource;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * copy of the original
 * club_topicana @{@link com.github.ftrossbach.club_topicana.core.TopicComparer}
 * using an {@link AdminClient} as parameter to support credentials
 */
public class TopicComparer {

   private AdminClient adminClient;

   public TopicComparer(AdminClient adminClient) {
      this.adminClient = adminClient;
   }

   public ComparisonResult compare(Collection<ExpectedTopicConfiguration> expectedTopicConfiguration) {

      ComparisonResult.ComparisonResultBuilder resultBuilder = new ComparisonResult.ComparisonResultBuilder();

      List<String> topicNames = expectedTopicConfiguration
            .stream()
            .map(ExpectedTopicConfiguration::getTopicName)
            .collect(toList());

      Map<String, TopicDescription> topicDescriptions = getTopicDescriptions(resultBuilder, topicNames);

      compareTopicDescriptions(expectedTopicConfiguration, resultBuilder, topicDescriptions);

      Map<String, Config> topicConfigs = getTopicConfigs(topicNames, topicDescriptions);

      compareTopicConfigs(expectedTopicConfiguration, resultBuilder, topicNames, topicDescriptions, topicConfigs);

      return resultBuilder.build();

   }

   private void compareTopicConfigs(Collection<ExpectedTopicConfiguration> expectedTopicConfiguration, ComparisonResult.ComparisonResultBuilder resultBuilder, List<String> topicNames, Map<String, TopicDescription> topicDescriptions, Map<String, Config> topicConfigs) {
      expectedTopicConfiguration.stream().filter(exp -> topicDescriptions.containsKey(topicNames)).forEach(exp -> {
         Config config = topicConfigs.get(exp.getTopicName());

         exp.getProps().entrySet().forEach(prop -> {
            ConfigEntry entry = config.get(prop.getKey());

            if (entry == null) {
               resultBuilder.addMismatchingConfiguration(exp.getTopicName(), prop.getKey(), prop.getValue(), null);
            } else if (!prop.getValue().equals(entry.value())) {
               resultBuilder
                     .addMismatchingConfiguration(exp.getTopicName(), prop.getKey(), prop.getValue(), entry.value());
            }
         });
      });
   }

   private Map<String, Config> getTopicConfigs(List<String> topicNames, Map<String, TopicDescription> topicDescriptions) {
      return adminClient
               .describeConfigs(topicNames
                     .stream()
                     .filter(exp -> topicDescriptions.containsKey(topicNames))
                     .map(this::topicNameToResource)
                     .collect(toList()))
               .values()
               .entrySet()
               .stream()

               .flatMap(tc -> {
                  Map<String, Config> res = new HashMap<>();
                  try {

                     res.put(tc.getKey().name(), tc.getValue().get());
                  } catch (InterruptedException e) {
                     Thread.currentThread().interrupt();
                     throw new EvaluationException("Interrupted Exception during adminClient.describeConfigs", e);
                  } catch (ExecutionException e) {
                     throw new EvaluationException("Exception during adminClient.describeConfigs", e);
                  }
                  return res.entrySet().stream();
               })
               .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
   }

   private void compareTopicDescriptions(Collection<ExpectedTopicConfiguration> expectedTopicConfiguration, ComparisonResult.ComparisonResultBuilder resultBuilder, Map<String, TopicDescription> topicDescriptions) {
      expectedTopicConfiguration
            .stream()
            .filter(exp -> topicDescriptions.containsKey(exp.getTopicName()))
            .forEach(exp -> {
               TopicDescription topicDescription = topicDescriptions.get(exp.getTopicName());

               if (exp.getPartitions().isSpecified()
                     && topicDescription.partitions().size() != exp.getPartitions().count()) {
                  resultBuilder
                        .addMismatchingPartitionCount(exp.getTopicName(), exp.getPartitions().count(),
                              topicDescription.partitions().size());
               }
               int repflicationFactor = topicDescription.partitions().stream().findFirst().get().replicas().size();
               if (exp.getReplicationFactor().isSpecified()
                     && repflicationFactor != exp.getReplicationFactor().count()) {
                  resultBuilder
                        .addMismatchingReplicationFactor(exp.getTopicName(), exp.getReplicationFactor().count(),
                              repflicationFactor);
               }

            });
   }

   private Map<String, TopicDescription> getTopicDescriptions(ComparisonResult.ComparisonResultBuilder resultBuilder, List<String> topicNames) {
      return adminClient
               .describeTopics(topicNames)
               .values()
               .entrySet()
               .stream()
               .flatMap(desc -> {
                  try {
                     TopicDescription topicDescription = desc.getValue().get();
                     return Collections.singletonList(topicDescription).stream();
                  } catch (ExecutionException e) {
                     resultBuilder.addMissingTopic(desc.getKey());
                     return Collections.<TopicDescription> emptySet().stream();
                  } catch (InterruptedException e) {
                     Thread.currentThread().interrupt();
                     throw new EvaluationException("InterruptedException during adminClient.describeTopics", e);
                  }
               })
               .collect(toMap(TopicDescription::name, i -> i));
   }

   private ConfigResource topicNameToResource(String topicName) {
      return new ConfigResource(ConfigResource.Type.TOPIC, topicName);
   }
}
