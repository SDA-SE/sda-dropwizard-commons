package org.sdase.commons.server.opa.testing;

import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sdase.commons.server.opa.testing.OpaRule.onAnyRequest;
import static org.sdase.commons.server.opa.testing.OpaRule.onRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.VerificationException;
import java.io.IOException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sdase.commons.server.opa.filter.model.OpaInput;
import org.sdase.commons.server.opa.filter.model.OpaRequest;
import org.sdase.commons.server.opa.filter.model.OpaResponse;
import org.sdase.commons.server.opa.testing.test.ConstraintModel;

public class OpaRuleIT {

   @ClassRule
   public static final OpaRule OPA_RULE = new OpaRule();

   private String path = "resources";
   private String method = "GET";

   @Before
   public void before() {
      OPA_RULE.reset();
   }

   @Test
   public void shouldReturnResponseOnMatch() {
      // given
      OPA_RULE.mock(onRequest(method, path).allow());
      // when
      Response response = requestMock(request(method, path));
      // then
      assertThat(response.getStatus()).isEqualTo(SC_OK);
      assertThat(response.readEntity(OpaResponse.class).getResult().isAllow()).isTrue();
      OPA_RULE.verify(1, method, path);
   }

   @Test
   public void shouldReturnResponseOnMatchOpaRequest() {
      // given
      OPA_RULE.mock(onRequest(method, path).allow());
      // when
      Response response = requestMock(request(method, path));
      // then
      assertThat(response.getStatus()).isEqualTo(SC_OK);
      OpaResponse opaResponse = response.readEntity(OpaResponse.class);
      assertThat(opaResponse.getResult().isAllow()).isTrue();
      assertThat(opaResponse.getResult().getConstraints()).isNull();
      OPA_RULE.verify(1, method, path);
   }

   @Test(expected = VerificationException.class)
   public void shouldThrowExceptionIfNotVerified() {
      OPA_RULE.verify(1, method, path);
   }

   @Test
   public void shouldReturnServerError() {
      // given
      OPA_RULE.mock(onAnyRequest().serverError());
      // when
      Response response = requestMock(request(method, path));
      // then
      assertThat(response.getStatus()).isEqualTo(SC_INTERNAL_SERVER_ERROR);
   }

   @Test
   public void shouldReturnEmptyResponse() {
      // given
      OPA_RULE.mock(onAnyRequest().emptyResponse());
      // when
      Response response = requestMock(request(method, path));
      // then
      assertThat(response.getStatus()).isEqualTo(SC_OK);
      assertThat(response.readEntity(String.class)).isNullOrEmpty();
   }

   @Test
   public void shouldMockLongPathSuccessfully() {
      // given
      OPA_RULE.mock(onRequest("GET", "/p1/p2").allow());

      // when
      Response response = requestMock(request("GET", "p1", "p2"));
      // then
      assertThat(response.getStatus()).isEqualTo(SC_OK);
      assertThat(response.readEntity(OpaResponse.class).getResult().isAllow()).isTrue();
   }

   @Test
   public void shouldReturnDifferentResponsesInOneTest() throws IOException {
      // given
      ConstraintModel constraintModel = new ConstraintModel().addConstraint("key", "A", "B");
      OPA_RULE
            .mock(onRequest("GET", "pathA")
                  .allow()
                  .withConstraint(constraintModel));
      OPA_RULE.mock(onRequest("POST", "pathB").deny());
      // when
      Response response = requestMock(request("GET", "pathA"));
      Response response2 = requestMock(request("POST", "pathB"));
      // then
      assertThat(response.getStatus()).isEqualTo(SC_OK);
      assertThat(response2.getStatus()).isEqualTo(SC_OK);
      OpaResponse opaResponse = response.readEntity(OpaResponse.class);
      assertThat(opaResponse.getResult().isAllow()).isTrue();
      assertThat(new ObjectMapper().readValue(opaResponse.getResult().getConstraints(), ConstraintModel.class)).isEqualToComparingFieldByFieldRecursively(constraintModel);
      OpaResponse opaResponse2 = response2.readEntity(OpaResponse.class);
      assertThat(opaResponse2.getResult().isAllow()).isFalse();
   }

   private Response requestMock(OpaRequest request) {
      return JerseyClientBuilder
            .createClient()
            .target(OPA_RULE.getUrl())
            .path("/v1/data/policy")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .post(Entity.json(request));
   }

   private OpaRequest request(String method, String... path) {
      return new OpaRequest().setInput(new OpaInput().setHttpMethod(method).setPath(path));
   }

}
