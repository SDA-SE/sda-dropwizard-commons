package org.sdase.commons.server.spring.data.mongo.converter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.sdase.commons.server.spring.data.mongo.annotation.DocumentType;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.convert.TypeInformationMapper;
import org.springframework.data.mapping.Alias;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;

public class AnnotationTypeInformationMapper implements TypeInformationMapper {

  private final Map<TypeInformation<?>, Alias> typeToAliasMap;
  private final Map<Alias, TypeInformation<?>> aliasToTypeMap;

  private AnnotationTypeInformationMapper(List<String> basePackagesToScan) {
    typeToAliasMap = new HashMap<>();
    aliasToTypeMap = new HashMap<>();

    populateTypeMap(basePackagesToScan);
  }

  private void populateTypeMap(List<String> basePackagesToScan) {
    ClassPathScanningCandidateComponentProvider scanner =
        new ClassPathScanningCandidateComponentProvider(false);

    scanner.addIncludeFilter(new AnnotationTypeFilter(DocumentType.class));

    for (String basePackage : basePackagesToScan) {
      for (BeanDefinition bd : scanner.findCandidateComponents(basePackage)) {
        try {
          Class<?> clazz = Class.forName(bd.getBeanClassName());
          DocumentType documentTypeAnnotation = clazz.getAnnotation(DocumentType.class);

          ClassTypeInformation<?> type = ClassTypeInformation.from(clazz);
          var alias = Alias.of(documentTypeAnnotation.value());

          typeToAliasMap.put(type, alias);
          aliasToTypeMap.put(alias, type);

        } catch (ClassNotFoundException e) {
          throw new IllegalStateException(
              String.format("Class [%s] could not be loaded.", bd.getBeanClassName()), e);
        }
      }
    }
  }

  @Nullable
  @Override
  public TypeInformation<?> resolveTypeFrom(Alias alias) {
    if (aliasToTypeMap.containsKey(alias)) {
      return aliasToTypeMap.get(alias);
    }
    return null;
  }

  @Override
  public Alias createAliasFor(TypeInformation<?> type) {
    ClassTypeInformation<?> typeClass = (ClassTypeInformation<?>) type;

    if (typeToAliasMap.containsKey(typeClass)) {
      return typeToAliasMap.get(typeClass);
    }

    return Alias.empty();
  }

  public static class Builder {
    List<String> basePackagesToScan;

    public Builder() {
      basePackagesToScan = new ArrayList<>();
    }

    public Builder withBasePackages(String[] basePackages) {
      basePackagesToScan.addAll(Arrays.asList(basePackages));
      return this;
    }

    public AnnotationTypeInformationMapper build() {
      return new AnnotationTypeInformationMapper(basePackagesToScan);
    }
  }
}
