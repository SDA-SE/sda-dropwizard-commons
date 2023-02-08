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

/**
 * @deprecated All classes from this package will be removed in the next version because topic
 *     comparison is not recommended.
 */
@Deprecated
public interface PartitionCount {

  boolean isSpecified();

  int count();

  static PartitionCount of(int count) {

    return new SpecifiedCount(count);
  }

  static PartitionCount ignore() {
    return new UnspecifiedCount();
  }

  class SpecifiedCount extends AbstractSpecifiedCount implements PartitionCount {

    private SpecifiedCount(int count) {
      super(count);
    }
  }

  class UnspecifiedCount implements PartitionCount {

    private UnspecifiedCount() {}

    @Override
    public boolean isSpecified() {
      return false;
    }

    @Override
    public int count() {
      throw new IllegalStateException("Must not call count() on an unspecified PartitionCount");
    }
  }
}
