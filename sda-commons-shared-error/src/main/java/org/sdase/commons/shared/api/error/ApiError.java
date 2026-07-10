package org.sdase.commons.shared.api.error;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Error Entity object for transferring error information between server and client.
 *
 * @deprecated This class will be replaced by {@link
 *     org.sdase.commons.server.dropwizard.model.error.ApiError} when removing the module {@code
 *     sda-commons-shared-error}. To prepare for the upcoming breaking change, update all references
 *     to {@link org.sdase.commons.server.dropwizard.model.error.ApiError} and remove direct
 *     dependencies to {@code sda-commons-shared-error}.
 */
@Schema(
    description =
        "Describes an api error object to transfer error information between server and client.",
    deprecated = false)
@Deprecated(forRemoval = true)
@SuppressWarnings("java:S2176") // intentionally the same name until removed
public class ApiError extends org.sdase.commons.server.dropwizard.model.error.ApiError {

  public ApiError() {
    // default constructor
  }

  public ApiError(String title) {
    super(title);
  }

  /**
   * @deprecated migrate to {@link
   *     org.sdase.commons.server.dropwizard.model.error.ApiError#ApiError(String, List)}. This
   *     constructor will copy the given list. The replacement keeps the old behavior which uses the
   *     same instance.
   */
  @Deprecated(forRemoval = true)
  public ApiError(
      String title,
      List<? extends org.sdase.commons.server.dropwizard.model.error.ApiInvalidParam>
          invalidParams) {
    super(
        title,
        invalidParams == null
            ? null
            : invalidParams.stream()
                .map(
                    p ->
                        new org.sdase.commons.server.dropwizard.model.error.ApiInvalidParam(
                            p.getField(), p.getReason(), p.getErrorCode()))
                .toList());
  }

  /**
   * @deprecated migrate to {@link
   *     org.sdase.commons.server.dropwizard.model.error.ApiError#getInvalidParams()}. This method
   *     will copy the internal list. The replacement keeps the old behavior which uses the same
   *     instance.
   */
  @Deprecated(forRemoval = true)
  @Override
  public List<ApiInvalidParam> getInvalidParams() {
    var invalidParams = super.getInvalidParams();
    return invalidParams == null
        ? null
        : invalidParams.stream()
            .map(p -> new ApiInvalidParam(p.getField(), p.getReason(), p.getErrorCode()))
            .toList();
  }
}
