package org.sdase.commons.server.dropwizard.model.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ApiInvalidParamTest {
  @Test
  void toStringShouldNotContainReason() {
    ApiInvalidParam value = new ApiInvalidParam("field", "reason", "errorCode");
    assertThat(value.toString()).contains("field", "errorCode").doesNotContain("reason");
  }
}
