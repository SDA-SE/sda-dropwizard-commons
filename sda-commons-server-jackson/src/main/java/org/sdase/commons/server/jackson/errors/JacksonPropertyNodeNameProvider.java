/*
 * Copyright 2020 Hibernate Validator Authors
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copied from https://github.com/hibernate/hibernate-validator/blob/2427912f7bf3ec698c9229e9920b50dfa83d21be/engine/src/test/java/org/hibernate/validator/test/spi/nodenameprovider/jackson/JacksonAnnotationPropertyNodeNameProvider.java
 */

package org.sdase.commons.server.jackson.errors;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import org.hibernate.validator.spi.nodenameprovider.JavaBeanProperty;
import org.hibernate.validator.spi.nodenameprovider.Property;
import org.hibernate.validator.spi.nodenameprovider.PropertyNodeNameProvider;

/**
 * A {@link PropertyNodeNameProvider} that resolves the name of the property via Jackson. By
 * default, Hibernate reports the name of the properties. With this changes, it resolves it to the
 * name of the property or of {@code @JsonProperty("...name...")}.
 */
public class JacksonPropertyNodeNameProvider implements PropertyNodeNameProvider {
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public String getName(Property property) {
    if (property instanceof JavaBeanProperty) {
      return getJavaBeanPropertyName((JavaBeanProperty) property);
    }

    return getDefaultName(property);
  }

  private String getJavaBeanPropertyName(JavaBeanProperty property) {
    JavaType type = objectMapper.constructType(property.getDeclaringClass());
    BeanDescription desc = objectMapper.getSerializationConfig().introspect(type);

    return desc.findProperties().stream()
        .filter(prop -> prop.getInternalName().equals(property.getName()))
        .map(BeanPropertyDefinition::getName)
        .findFirst()
        .orElse(property.getName());
  }

  private String getDefaultName(Property property) {
    return property.getName();
  }
}
