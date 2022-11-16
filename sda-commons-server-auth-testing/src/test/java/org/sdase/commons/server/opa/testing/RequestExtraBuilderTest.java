package org.sdase.commons.server.opa.testing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import org.junit.jupiter.api.Test;
import org.sdase.commons.server.opa.filter.model.OpaResponse;

class RequestExtraBuilderTest {

  private String lastReceivedJwt;
  private boolean withJwtCalled;

  private OpaRule.RequestExtraBuilder dummyImpl =
      new OpaRule.RequestExtraBuilder() {
        @Override
        public OpaRule.RequestExtraBuilder withJwt(String jwt) {
          lastReceivedJwt = jwt;
          withJwtCalled = true;
          return this;
        }

        @Override
        public OpaRule.FinalBuilder allow() {
          // not necessary for this test
          return null;
        }

        @Override
        public OpaRule.FinalBuilder deny() {
          // not necessary for this test
          return null;
        }

        @Override
        public OpaRule.BuildBuilder answer(OpaResponse response) {
          // not necessary for this test
          return null;
        }

        @Override
        public OpaRule.BuildBuilder emptyResponse() {
          // not necessary for this test
          return null;
        }

        @Override
        public OpaRule.BuildBuilder serverError() {
          // not necessary for this test
          return null;
        }
      };

  // tests for withJwtHeaderValue(String)

  @Test
  void shouldStripBearerFromHeaderValue() {

    String given = "Bearer ey...ey...sig";

    dummyImpl.withJwtFromHeaderValue(given);

    assertThat(lastReceivedJwt).isEqualTo("ey...ey...sig");
  }

  @Test
  void shouldStripBearerInLowerCaseFromHeaderValue() {

    String given = "bearer ey...ey...sig";

    dummyImpl.withJwtFromHeaderValue(given);

    assertThat(lastReceivedJwt).isEqualTo("ey...ey...sig");
  }

  @Test
  void shouldStripBearerWithManySpacesFromHeaderValue() {

    String given = "Bearer   ey...ey...sig";

    dummyImpl.withJwtFromHeaderValue(given);

    assertThat(lastReceivedJwt).isEqualTo("ey...ey...sig");
  }

  @Test
  void shouldSilentlyIgnoreNullHeaderValue() {

    assertThatCode(() -> dummyImpl.withJwtFromHeaderValue(null)).doesNotThrowAnyException();
    assertThat(withJwtCalled).isFalse();
  }

  @Test
  void shouldSilentlyIgnoreBlankHeaderValue() {

    assertThatCode(() -> dummyImpl.withJwtFromHeaderValue("   ")).doesNotThrowAnyException();
    assertThat(withJwtCalled).isFalse();
  }

  @Test
  void shouldReturnImplementationInstanceOnHeaderValue() {

    OpaRule.RequestExtraBuilder requestExtraBuilder =
        dummyImpl.withJwtFromHeaderValue("Bearer ey...ey...sig");

    assertThat(requestExtraBuilder).isSameAs(dummyImpl.withJwt("Bearer ey...ey...sig"));
  }

  @Test
  void shouldReturnImplementationInstanceOnIgnoreHeaderValue() {

    OpaRule.RequestExtraBuilder requestExtraBuilder = dummyImpl.withJwtFromHeaderValue(null);

    assertThat(requestExtraBuilder).isSameAs(dummyImpl);
  }

  // tests for withJwtFromHeaders(MultivaluedMap) with single value

  @Test
  void shouldStripBearerFromHeaders() {

    String given = "Bearer ey...ey...sig";

    MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
    headers.add("Authorization", given);

    dummyImpl.withJwtFromHeaders(headers);

    assertThat(lastReceivedJwt).isEqualTo("ey...ey...sig");
  }

  @Test
  void shouldStripBearerInLowerCaseFromHeaders() {

    String given = "bearer ey...ey...sig";

    MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
    headers.add("Authorization", given);

    dummyImpl.withJwtFromHeaders(headers);

    assertThat(lastReceivedJwt).isEqualTo("ey...ey...sig");
  }

  @Test
  void shouldStripBearerWithManySpacesFromHeaders() {

    String given = "Bearer   ey...ey...sig";

    MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
    headers.add("Authorization", given);

    dummyImpl.withJwtFromHeaders(headers);

    assertThat(lastReceivedJwt).isEqualTo("ey...ey...sig");
  }

  @Test
  void shouldSilentlyIgnoreEmptyHeaders() {

    MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();

    assertThatCode(() -> dummyImpl.withJwtFromHeaders(headers)).doesNotThrowAnyException();
    assertThat(withJwtCalled).isFalse();
  }

  @Test
  void shouldReturnImplementationInstanceOnHeaders() {

    MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
    headers.add("Authorization", "Bearer ey...ey...sig");

    OpaRule.RequestExtraBuilder requestExtraBuilder = dummyImpl.withJwtFromHeaders(headers);

    assertThat(requestExtraBuilder).isSameAs(dummyImpl.withJwt("Bearer ey...ey...sig"));
  }

  @Test
  void shouldReturnImplementationInstanceOnIgnoreHeaders() {

    MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();

    OpaRule.RequestExtraBuilder requestExtraBuilder = dummyImpl.withJwtFromHeaders(headers);

    assertThat(requestExtraBuilder).isSameAs(dummyImpl);
  }
}
