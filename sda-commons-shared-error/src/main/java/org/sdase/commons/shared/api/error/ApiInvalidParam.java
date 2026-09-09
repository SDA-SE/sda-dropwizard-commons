package org.sdase.commons.shared.api.error;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Invalid parameter information within an {@link ApiError}
 *
 * @deprecated This class will be replaced by {@link
 *     org.sdase.commons.server.dropwizard.model.error.ApiInvalidParam} when removing the module
 *     {@code sda-commons-shared-error}. To prepare for the upcoming breaking change, update all
 *     references to {@link org.sdase.commons.server.dropwizard.model.error.ApiInvalidParam} and
 *     remove direct dependencies to {@code sda-commons-shared-error}.
 */
@Schema(description = "Defines a validation error for a parameter or field.", deprecated = false)
@Deprecated(forRemoval = true)
@SuppressWarnings("java:S2176") // intentionally the same name until removed
public class ApiInvalidParam
    extends org.sdase.commons.server.dropwizard.model.error.ApiInvalidParam {

  public ApiInvalidParam() {
    super();
  }

  public ApiInvalidParam(String field, String reason, String errorCode) {
    super(field, reason, errorCode);
  }
}
