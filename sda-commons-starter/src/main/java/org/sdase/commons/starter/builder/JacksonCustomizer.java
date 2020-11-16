package org.sdase.commons.starter.builder;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.Configuration;
import java.time.ZonedDateTime;
import java.util.function.Consumer;
import org.sdase.commons.server.jackson.EnableFieldFilter;
import org.sdase.commons.server.jackson.Iso8601Serializer;

public interface JacksonCustomizer<C extends Configuration> {

  /**
   * Skips registration of the HAL module. This may be used when links and embedded resources are
   * not required or are achieved with other tooling.
   *
   * @return the builder
   */
  PlatformBundleBuilder<C> withoutHalSupport();

  /**
   * Disables the field filter entirely. The field filter may be used by clients to request only a
   * subset of the properties of a resource and has to be activated with {@link EnableFieldFilter}
   * for each resource.
   *
   * @return the builder
   */
  PlatformBundleBuilder<C> withoutFieldFilter();

  /**
   * Allows customization of the used {@link ObjectMapper}. More customizers may be added by calling
   * this method multiple times.
   *
   * @param customizer receives the used {@link ObjectMapper} for customization, e.g. to enable or
   *     disable specific features or configure formatting.
   * @return the builder
   */
  PlatformBundleBuilder<C> withObjectMapperCustomization(Consumer<ObjectMapper> customizer);

  /**
   * Registers a default serializer for {@link ZonedDateTime} that renders 3 digits of milliseconds.
   * The same serializer may be configured per field as documented in {@link
   * Iso8601Serializer.WithMillis}.
   *
   * <p>This setting overwrites the default behaviour of Jackson which omits milliseconds if they
   * are zero or adds nanoseconds if they are set.
   *
   * @return the builder
   */
  PlatformBundleBuilder<C> alwaysWriteZonedDateTimeWithMillisInJson();

  /**
   * Registers a default serializer for {@link ZonedDateTime} that renders no milliseconds. The same
   * serializer may be configured per field as documented in {@link Iso8601Serializer}.
   *
   * <p>This setting overwrites the default behaviour of Jackson which omits milliseconds if they
   * are zero or adds nanoseconds if they are set.
   *
   * @return the builder
   */
  PlatformBundleBuilder<C> alwaysWriteZonedDateTimeWithoutMillisInJson();
}
