package org.sdase.commons.server.consumer;

import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import javax.validation.constraints.NotNull;
import org.sdase.commons.server.consumer.filter.ConsumerTokenServerFilter;

public class ConsumerTokenBundle<C extends Configuration> implements ConfiguredBundle<C> {

  private final Function<C, ConsumerTokenConfig> consumerTokenConfigProvider;

  private ConsumerTokenBundle(Function<C, ConsumerTokenConfig> consumerTokenConfigProvider) {
    this.consumerTokenConfigProvider = consumerTokenConfigProvider;
  }

  @Override
  public void initialize(Bootstrap<?> bootstrap) {
    // nothing to initialize
  }

  @Override
  public void run(C configuration, Environment environment) {
    ConsumerTokenConfig config = consumerTokenConfigProvider.apply(configuration);
    boolean requireIdentifiedConsumer = !config.isOptional();
    ConsumerTokenServerFilter consumerTokenServerFilter =
        new ConsumerTokenServerFilter(requireIdentifiedConsumer, config.getExcludePatterns());
    environment.jersey().register(consumerTokenServerFilter);
  }

  public static OptionsBuilder builder() {
    return new Builder<>();
  }

  @FunctionalInterface
  public interface ConsumerTokenConfigProvider<C extends Configuration>
      extends Function<C, ConsumerTokenConfig> {}

  //
  // builder
  //

  public interface OptionsBuilder {
    /**
     * Creates the bundle with the consumer token being optional.
     *
     * @return a builder to create the bundle
     */
    FinalBuilder<Configuration> withOptionalConsumerToken();

    /**
     * Creates the bundle with a consumer token required for every request.
     *
     * @return a builder to create the bundle
     */
    ExcludeBuilder<Configuration> withRequiredConsumerToken();

    /**
     * @param configProvider the method returning the {@link ConsumerTokenConfig} from the
     *     applications {@link Configuration}.
     * @param <C> the applications configuration type
     * @return a builder to create the bundle
     */
    <C extends Configuration> FinalBuilder<C> withConfigProvider(
        ConsumerTokenConfigProvider<C> configProvider);
  }

  public interface ExcludeBuilder<C extends Configuration> extends FinalBuilder<C> {

    /**
     * Creates the bundle but ignores urls that matches the given exclude pattern regex
     *
     * @param regex regex that must be includeed
     * @return a builder for further options
     */
    FinalBuilder<C> withExcludePatterns(@NotNull String... regex);
  }

  public interface FinalBuilder<C extends Configuration> {
    ConsumerTokenBundle<C> build();
  }

  public static class Builder<C extends Configuration>
      implements FinalBuilder<C>, OptionsBuilder, ExcludeBuilder<C> {

    private Function<C, ConsumerTokenConfig> configProvider;

    private Boolean requireConsumerToken;

    private List<String> excludePattern = new ArrayList<>();

    private Builder() {}

    private Builder(boolean requireConsumerToken, List<String> excludePattern) {
      this.requireConsumerToken = requireConsumerToken;
      this.excludePattern = excludePattern;
    }

    private Builder(ConsumerTokenConfigProvider<C> configProvider) {
      this.configProvider = configProvider;
    }

    @Override
    public <T extends Configuration> FinalBuilder<T> withConfigProvider(
        ConsumerTokenConfigProvider<T> configProvider) {
      return new Builder<>(configProvider);
    }

    @Override
    public FinalBuilder<Configuration> withOptionalConsumerToken() {
      return new Builder<>(false, this.excludePattern);
    }

    @Override
    public ExcludeBuilder<Configuration> withRequiredConsumerToken() {
      return new Builder<>(true, excludePattern);
    }

    @Override
    public FinalBuilder<C> withExcludePatterns(String... pattern) {
      return new Builder<>(this.requireConsumerToken, Arrays.asList(pattern));
    }

    @Override
    public ConsumerTokenBundle<C> build() {
      if (configProvider == null) {
        if (requireConsumerToken == null) {
          throw new IllegalStateException("Missing either a config provider or an explicit config");
        }
        ConsumerTokenConfig consumerTokenConfig = new ConsumerTokenConfig();
        consumerTokenConfig.setOptional(!requireConsumerToken);
        consumerTokenConfig.setExcludePatterns(excludePattern);
        configProvider = c -> consumerTokenConfig;
      }
      if (excludeOpenApi()) {
        configProvider = configProvider.andThen(this::addOpenApiConfigExclude);
      }
      return new ConsumerTokenBundle<>(configProvider);
    }

    private ConsumerTokenConfig addOpenApiConfigExclude(ConsumerTokenConfig consumerTokenConfig) {
      ArrayList<String> patterns = new ArrayList<>(consumerTokenConfig.getExcludePatterns());
      patterns.add("openapi\\.(json|yaml)");
      consumerTokenConfig.setExcludePatterns(patterns);
      return consumerTokenConfig;
    }

    private boolean excludeOpenApi() {
      try {
        if (getClass().getClassLoader().loadClass("org.sdase.commons.server.openapi.OpenApiBundle")
            != null) {
          return true;
        }
      } catch (ClassNotFoundException e) {
        // silently ignored
      }
      return false;
    }
  }
}
