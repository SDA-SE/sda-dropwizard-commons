package org.sdase.commons.server.prometheus.mapping;

import io.prometheus.client.Collector;

import io.prometheus.client.dropwizard.samplebuilder.DefaultSampleBuilder;
import io.prometheus.client.dropwizard.samplebuilder.SampleBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// TODO: This class is copied from https://github.com/prometheus/client_java/tree/master/simpleclient_dropwizard/src/main/java/io/prometheus/client/dropwizard/samplebuilder
//  till the bug fix is released in version 0.7.0.

public class DropwizardSampleBuilder implements SampleBuilder {
   private final List<CompiledMapperConfig> compiledMapperConfigs;
   private final DefaultSampleBuilder defaultMetricSampleBuilder = new DefaultSampleBuilder();

  public DropwizardSampleBuilder(final List<MapperConfig> mapperConfigs) {
    if (mapperConfigs == null || mapperConfigs.isEmpty()) {
      throw new IllegalArgumentException("CustomMappingSampleBuilder needs some mapper configs!");
    }

    this.compiledMapperConfigs = new ArrayList<CompiledMapperConfig>(mapperConfigs.size());
    for (MapperConfig config : mapperConfigs) {
      this.compiledMapperConfigs.add(new CompiledMapperConfig(config));
    }
  }

   @Override
   public Collector.MetricFamilySamples.Sample createSample(final String dropwizardName, final String nameSuffix,
         final List<String> additionalLabelNames, final List<String> additionalLabelValues, final double value) {
      if (dropwizardName == null) {
         throw new IllegalArgumentException("Dropwizard metric name cannot be null");
      }

      CompiledMapperConfig matchingConfig = null;
      for (CompiledMapperConfig config : this.compiledMapperConfigs) {
         if (config.pattern.matches(dropwizardName)) {
            matchingConfig = config;
            break;
         }
      }

      if (matchingConfig != null) {
         final Map<String, String> params = matchingConfig.pattern.extractParameters(dropwizardName);
         final NameAndLabels nameAndLabels = getNameAndLabels(matchingConfig.mapperConfig, params);
         nameAndLabels.labelNames.addAll(additionalLabelNames);
         nameAndLabels.labelValues.addAll(additionalLabelValues);
         return defaultMetricSampleBuilder
               .createSample(nameAndLabels.name, nameSuffix, nameAndLabels.labelNames, nameAndLabels.labelValues,
                     value);
      }

      return defaultMetricSampleBuilder
            .createSample(dropwizardName, nameSuffix, additionalLabelNames, additionalLabelValues, value);
   }

   protected NameAndLabels getNameAndLabels(final MapperConfig config, final Map<String, String> parameters) {
      final String metricName = formatTemplate(config.getName(), parameters);
      final List<String> labels = new ArrayList<String>(config.getLabels().size());
      final List<String> labelValues = new ArrayList<String>(config.getLabels().size());
      for (Map.Entry<String, String> entry : config.getLabels().entrySet()) {
         labels.add(entry.getKey());
         labelValues.add(formatTemplate(entry.getValue(), parameters));
      }

      return new NameAndLabels(metricName, labels, labelValues);
   }

   private String formatTemplate(final String template, final Map<String, String> params) {
      String result = template;
      for (Map.Entry<String, String> entry : params.entrySet()) {
         result = result.replace(entry.getKey(), entry.getValue());
      }

      return result;
   }

   static class CompiledMapperConfig {
      final MapperConfig mapperConfig;
      final GraphiteNamePattern pattern;

      CompiledMapperConfig(final MapperConfig mapperConfig) {
         this.mapperConfig = mapperConfig;
         this.pattern = new GraphiteNamePattern(mapperConfig.getMatch());
      }
   }

   static class NameAndLabels {
      final String name;
      final List<String> labelNames;
      final List<String> labelValues;

      NameAndLabels(final String name, final List<String> labelNames, final List<String> labelValues) {
         this.name = name;
         this.labelNames = labelNames;
         this.labelValues = labelValues;
      }
   }
}
