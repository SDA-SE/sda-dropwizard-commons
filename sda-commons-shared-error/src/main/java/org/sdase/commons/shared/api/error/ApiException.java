package org.sdase.commons.shared.api.error;

/**
 * Exception that should be thrown within rest services.
 *
 * @deprecated This class will be replaced by {@link
 *     org.sdase.commons.server.dropwizard.model.error.ApiException} when removing the module {@code
 *     sda-commons-shared-error}. To prepare for the upcoming breaking change, update all references
 *     to {@link org.sdase.commons.server.dropwizard.model.error.ApiException} and remove direct
 *     dependencies to {@code sda-commons-shared-error}.
 */
@Deprecated(forRemoval = true)
@SuppressWarnings("java:S2176") // intentionally the same name until removed
public class ApiException extends org.sdase.commons.server.dropwizard.model.error.ApiException {

  private ApiException(org.sdase.commons.server.dropwizard.model.error.ApiException apiException) {
    super(apiException);
  }

  public ApiError getDTO() {
    return new ApiError(super.getDTO().getTitle(), super.getDTO().getInvalidParams());
  }

  /**
   * @deprecated migrate to {@link
   *     org.sdase.commons.server.dropwizard.model.error.ApiException#builder()};
   */
  @Deprecated(forRemoval = true)
  public static HttpCodeBuilder builder() {
    return new Builder();
  }

  /**
   * @deprecated migrate to {@link
   *     org.sdase.commons.server.dropwizard.model.error.ApiException.FinalBuilder};
   */
  @Deprecated(forRemoval = true)
  public interface FinalBuilder
      extends org.sdase.commons.server.dropwizard.model.error.ApiException.FinalBuilder {
    FinalBuilder detail(String field, String reason, String errorCode);

    FinalBuilder cause(Throwable cause);

    ApiException build();
  }

  /**
   * @deprecated migrate to {@link
   *     org.sdase.commons.server.dropwizard.model.error.ApiException.HttpCodeBuilder};
   */
  @Deprecated(forRemoval = true)
  public interface HttpCodeBuilder
      extends org.sdase.commons.server.dropwizard.model.error.ApiException.HttpCodeBuilder {
    TitleBuilder httpCode(int code);
  }

  /**
   * @deprecated migrate to {@link
   *     org.sdase.commons.server.dropwizard.model.error.ApiException.TitleBuilder};
   */
  @Deprecated(forRemoval = true)
  public interface TitleBuilder
      extends org.sdase.commons.server.dropwizard.model.error.ApiException.TitleBuilder {
    FinalBuilder title(String title);
  }

  /**
   * @deprecated migrate to {@link
   *     org.sdase.commons.server.dropwizard.model.error.ApiException.Builder};
   */
  @Deprecated(forRemoval = true)
  public static class Builder
      extends org.sdase.commons.server.dropwizard.model.error.ApiException.Builder
      implements TitleBuilder, HttpCodeBuilder, FinalBuilder {

    @Override
    public FinalBuilder detail(String field, String reason, String errorCode) {
      super.detail(field, reason, errorCode);
      return this;
    }

    @Override
    public FinalBuilder cause(Throwable cause) {
      super.cause(cause);
      return this;
    }

    @Override
    public ApiException build() {
      return new ApiException(super.build());
    }

    @Override
    public TitleBuilder httpCode(int code) {
      super.httpCode(code);
      return this;
    }

    @Override
    public FinalBuilder title(String title) {
      super.title(title);
      return this;
    }
  }
}
