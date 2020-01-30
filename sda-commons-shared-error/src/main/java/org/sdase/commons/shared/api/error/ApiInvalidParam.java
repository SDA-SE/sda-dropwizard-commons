package org.sdase.commons.shared.api.error;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/** Invalid parameter information within an {@link ApiError} */
@ApiModel(description = "Defines a validation error for a parameter or field.")
public class ApiInvalidParam {

  @ApiModelProperty(
      value = "The name or path of the invalid field or parameter.",
      example = "manufacture")
  private String field;

  @ApiModelProperty(
      value =
          "Gives a hint why the value is not valid. This is the error message of the validation. "
              + "The reason might be in different language due to internationalization.",
      example = "Audi has no Golf GTI model (not found)")
  private String reason;

  @ApiModelProperty(
      value = "The name of the validation annotation given in uppercase, underscore notation.",
      example = "FIELD_CORRELATION_ERROR")
  private String errorCode;

  public ApiInvalidParam() {
    // public constructor
  }

  public ApiInvalidParam(String field, String reason, String errorCode) {
    this.field = field;
    this.reason = reason;
    this.errorCode = errorCode;
  }

  public String getField() {
    return field;
  }

  public String getReason() {
    return reason;
  }

  public String getErrorCode() {
    return errorCode;
  }
}
