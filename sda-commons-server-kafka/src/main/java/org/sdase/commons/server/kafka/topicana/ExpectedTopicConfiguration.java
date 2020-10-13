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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Created by ftr on 10.11.17. */
public class ExpectedTopicConfiguration {

  private final String topicName;
  private final PartitionCount partitions;
  private final ReplicationFactor replicationFactor;
  private final Map<String, String> props;

  protected ExpectedTopicConfiguration(
      String topicName,
      PartitionCount partitions,
      ReplicationFactor replicationFactor,
      Map<String, String> props) {

    this.topicName = topicName;
    this.partitions = partitions;
    this.replicationFactor = replicationFactor;
    this.props = Collections.unmodifiableMap(props);
  }

  public String getTopicName() {
    return topicName;
  }

  public PartitionCount getPartitions() {
    return partitions;
  }

  public ReplicationFactor getReplicationFactor() {
    return replicationFactor;
  }

  public Map<String, String> getProps() {
    return props;
  }

  public static class ExpectedTopicConfigurationBuilder {
    private final String topicName;
    private PartitionCount partitions = PartitionCount.ignore();
    private ReplicationFactor replicationFactor = ReplicationFactor.ignore();
    private Map<String, String> props = new HashMap<>();

    public ExpectedTopicConfigurationBuilder(String topicName) {
      this.topicName = topicName;
    }

    public ExpectedTopicConfigurationBuilder withPartitionCount(int partitions) {
      this.partitions = PartitionCount.of(partitions);
      return this;
    }

    public ExpectedTopicConfigurationBuilder withReplicationFactor(int replicationFactor) {
      this.replicationFactor = ReplicationFactor.of(replicationFactor);
      return this;
    }

    public ExpectedTopicConfigurationBuilder withConfig(Map<String, String> props) {
      this.props.putAll(props);
      return this;
    }

    public ExpectedTopicConfigurationBuilder withConfig(String key, String value) {
      this.props.put(key, value);
      return this;
    }

    public ExpectedTopicConfiguration build() {
      return new ExpectedTopicConfiguration(topicName, partitions, replicationFactor, props);
    }
  }
}
