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

public class MismatchedTopicConfigException extends RuntimeException {

  private final ComparisonResult result;

  public MismatchedTopicConfigException(ComparisonResult result) {
    super("Topic configuration does not match specification: " + result.toString());
    this.result = result;
  }

  public ComparisonResult getResult() {
    return result;
  }
}
