package org.sdase.commons.server.consumer;

import io.dropwizard.core.Configuration;
import jakarta.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.List;

/**
 * @deprecated This class will be replaced by {@link
 *     org.sdase.commons.server.dropwizard.bundles.ConsumerTokenBundle} when removing the module
 *     {@code sda-commons-server-consumer}. To prepare for the upcoming breaking change, update all
 *     references to {@link org.sdase.commons.server.dropwizard.bundles.ConsumerTokenBundle} and
 *     remove direct dependencies to {@code sda-commons-server-consumer}.
 */
@Deprecated(forRemoval = true)
@SuppressWarnings("java:S2176") // intentionally the same name until removed
public class ConsumerTokenBundle<C extends Configuration>
    extends org.sdase.commons.server.dropwizard.bundles.ConsumerTokenBundle<C> {

  private ConsumerTokenBundle(
      org.sdase.commons.server.dropwizard.bundles.ConsumerTokenBundle<C> bundle) {
    super(bundle);
  }

  public static OptionsBuilder builder() {
    return new Builder<>();
  }

  @FunctionalInterface
  public interface ConsumerTokenConfigProvider<C extends Configuration>
      extends org.sdase.commons.server.dropwizard.bundles.ConsumerTokenBundle
              .ConsumerTokenConfigProvider<
          C> {}

  //
  // builder
  //

  public interface OptionsBuilder
      extends org.sdase.commons.server.dropwizard.bundles.ConsumerTokenBundle.OptionsBuilder {
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

  public interface ExcludeBuilder<C extends Configuration>
      extends org.sdase.commons.server.dropwizard.bundles.ConsumerTokenBundle.ExcludeBuilder<C>,
          FinalBuilder<C> {

    /**
     * Creates the bundle but ignores urls that matches the given exclude pattern regex
     *
     * @param regex regex that must be includeed
     * @return a builder for further options
     */
    FinalBuilder<C> withExcludePatterns(@NotNull String... regex);
  }

  public interface FinalBuilder<C extends Configuration>
      extends org.sdase.commons.server.dropwizard.bundles.ConsumerTokenBundle.FinalBuilder<C> {
    ConsumerTokenBundle<C> build();
  }

  public static class Builder<C extends Configuration>
      extends org.sdase.commons.server.dropwizard.bundles.ConsumerTokenBundle.Builder<C>
      implements FinalBuilder<C>, OptionsBuilder, ExcludeBuilder<C> {

    private Builder() {
      super();
    }

    private Builder(boolean requireConsumerToken, List<String> excludePattern) {
      super(requireConsumerToken, excludePattern);
    }

    private Builder(ConsumerTokenConfigProvider<C> configProvider) {
      super(configProvider::apply);
    }

    @Override
    public <T extends Configuration> FinalBuilder<T> withConfigProvider(
        ConsumerTokenConfigProvider<T> configProvider) {
      return new Builder<>(configProvider);
    }

    @Override
    public FinalBuilder<Configuration> withOptionalConsumerToken() {
      return new Builder<>(false, excludePattern);
    }

    @Override
    public ExcludeBuilder<Configuration> withRequiredConsumerToken() {
      return new Builder<>(true, excludePattern);
    }

    @Override
    public FinalBuilder<C> withExcludePatterns(String... pattern) {
      return new Builder<>(requireConsumerToken, Arrays.asList(pattern));
    }

    @Override
    @SuppressWarnings("java:S2440")
    public ConsumerTokenBundle<C> build() {
      return new ConsumerTokenBundle<>(super.build());
    }
  }
}
