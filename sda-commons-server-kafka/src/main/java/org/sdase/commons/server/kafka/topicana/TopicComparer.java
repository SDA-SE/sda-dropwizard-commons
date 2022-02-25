/**
 * Copyright © 2017 Florian Troßbach (trossbach@gmail.com)
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sdase.commons.server.kafka.topicana;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.sdase.commons.server.kafka.KafkaConfiguration;
import org.sdase.commons.server.kafka.KafkaProperties;

/**
 * This is a copy of the original {@code com.github.ftrossbach.club_topicana.core.TopicComparer}
 *
 * <p>Changes made:
 *
 * <ul>
 *   <li>Include Kafka Configuration to support a configurable admin client, for example for Kafka
 *       broker that require auth credentials
 *   <li>Refactoring of code to reduce complexity
 * </ul>
 */
public class TopicComparer {

  public TopicComparer() {
    // simple constructor
  }

  public ComparisonResult compare(
      Collection<ExpectedTopicConfiguration> expectedTopicConfiguration,
      KafkaConfiguration configuration) {
    try (final AdminClient adminClient =
        AdminClient.create(KafkaProperties.forAdminClient(configuration))) {

      ComparisonResult.ComparisonResultBuilder resultBuilder =
          new ComparisonResult.ComparisonResultBuilder();

      List<String> topicNames =
          expectedTopicConfiguration.stream()
              .map(ExpectedTopicConfiguration::getTopicName)
              .collect(toList());

      Map<String, TopicDescription> topicDescriptions =
          getTopicDescriptions(resultBuilder, topicNames, adminClient);

      compareTopicDescriptions(expectedTopicConfiguration, resultBuilder, topicDescriptions);

      Map<String, Config> topicConfigs =
          getTopicConfigs(topicNames, topicDescriptions, adminClient);

      compareTopicConfigs(
          expectedTopicConfiguration, resultBuilder, topicDescriptions, topicConfigs);

      return resultBuilder.build();
    }
  }

  private void compareTopicConfigs(
      Collection<ExpectedTopicConfiguration> expectedTopicConfiguration,
      ComparisonResult.ComparisonResultBuilder resultBuilder,
      Map<String, TopicDescription> topicDescriptions,
      Map<String, Config> topicConfigs) {

    expectedTopicConfiguration.stream()
        .filter(exp -> topicDescriptions.containsKey(exp.getTopicName()))
        .forEach(
            exp -> {
              Config config = topicConfigs.get(exp.getTopicName());

              exp.getProps()
                  .forEach(
                      (key, value) -> {
                        ConfigEntry entry = config.get(key);

                        if (entry == null) {
                          resultBuilder.addMismatchingConfiguration(
                              exp.getTopicName(), key, value, null);
                        } else if (!value.equals(entry.value())) {
                          resultBuilder.addMismatchingConfiguration(
                              exp.getTopicName(), key, value, entry.value());
                        }
                      });
            });
  }

  private Map<String, Config> getTopicConfigs(
      List<String> topicNames,
      Map<String, TopicDescription> topicDescriptions,
      AdminClient adminClient) {
    return adminClient
        .describeConfigs(
            topicNames.stream()
                .filter(topicDescriptions::containsKey)
                .map(this::topicNameToResource)
                .collect(toList()))
        .values()
        .entrySet()
        .stream()
        .flatMap(
            tc -> {
              Map<String, Config> res = new HashMap<>();
              try {

                res.put(tc.getKey().name(), tc.getValue().get());
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new EvaluationException(
                    "Interrupted Exception during adminClient.describeConfigs", e);
              } catch (ExecutionException e) {
                throw new EvaluationException("Exception during adminClient.describeConfigs", e);
              }
              return res.entrySet().stream();
            })
        .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private void compareTopicDescriptions(
      Collection<ExpectedTopicConfiguration> expectedTopicConfiguration,
      ComparisonResult.ComparisonResultBuilder resultBuilder,
      Map<String, TopicDescription> topicDescriptions) {
    expectedTopicConfiguration.stream()
        .filter(exp -> topicDescriptions.containsKey(exp.getTopicName()))
        .forEach(
            exp -> {
              TopicDescription topicDescription = topicDescriptions.get(exp.getTopicName());

              if (exp.getPartitions().isSpecified()
                  && topicDescription.partitions().size() != exp.getPartitions().count()) {
                resultBuilder.addMismatchingPartitionCount(
                    exp.getTopicName(),
                    exp.getPartitions().count(),
                    topicDescription.partitions().size());
              }
              if (!topicDescription.partitions().isEmpty()) {
                int repflicationFactor = topicDescription.partitions().get(0).replicas().size();
                if (exp.getReplicationFactor().isSpecified()
                    && repflicationFactor != exp.getReplicationFactor().count()) {
                  resultBuilder.addMismatchingReplicationFactor(
                      exp.getTopicName(), exp.getReplicationFactor().count(), repflicationFactor);
                }
              }
            });
  }

  private Map<String, TopicDescription> getTopicDescriptions(
      ComparisonResult.ComparisonResultBuilder resultBuilder,
      List<String> topicNames,
      AdminClient adminClient) {
    return adminClient.describeTopics(topicNames).values().entrySet().stream()
        .flatMap(
            desc -> {
              try {
                TopicDescription topicDescription = desc.getValue().get();
                return Stream.of(topicDescription);
              } catch (ExecutionException e) {
                if (e.getCause() instanceof UnknownTopicOrPartitionException) {
                  resultBuilder.addMissingTopic(desc.getKey());
                  return Stream.empty();
                } else {
                  throw (KafkaException) e.getCause();
                }
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new EvaluationException(
                    "InterruptedException during adminClient.describeTopics", e);
              }
            })
        .collect(toMap(TopicDescription::name, i -> i));
  }

  private ConfigResource topicNameToResource(String topicName) {
    return new ConfigResource(ConfigResource.Type.TOPIC, topicName);
  }
}
