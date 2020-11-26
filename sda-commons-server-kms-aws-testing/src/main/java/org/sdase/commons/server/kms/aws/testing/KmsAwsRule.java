package org.sdase.commons.server.kms.aws.testing;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import org.apache.commons.codec.binary.Base64;
import org.junit.rules.ExternalResource;
import org.junit.rules.RuleChain;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.sdase.commons.server.kms.aws.testing.model.ExtendedDecryptResult;
import org.sdase.commons.server.kms.aws.testing.model.ExtendedGenerateDataKeyResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KmsAwsRule extends ExternalResource {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String MEDIA_TYPE_AWS = "application/x-amz-json-1.1";

  private static final byte[] PK_PLAIN_TEXT =
      Base64.decodeBase64("m6J/7mPNnPGzpkLtnYx56YaZfZAiiQ/1VhBQbEY2UU4=");
  private static final byte[] CIPHER_TEXT_BLOB =
      Base64.decodeBase64(
          "TWFybjphd3M6a21zOnRlc3QtcmVnaW9uOnRlc3QtYWNjb3VudDprZXkvODUyZDU0OGUtODU5MC00Y2YzLWI3YjEtMDUyZTNjZDJjMWVhAAAAABNWrXdGHADmi0gpuwOBNlFduNg5CAA1XQ+mSzeWSlWzceeWJxTtlym3Yhcm25ZQYIP2DF0nXkMDbYr/NQ==");

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final WireMockClassRule wire =
      new WireMockClassRule(
          wireMockConfig().dynamicPort().extensions(new ResponseTemplateTransformer(false)));

  public KmsAwsRule() {
    try {
      initWireMocks();
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Unable to initialize.", e);
    }
  }

  @Override
  protected void before() {
    wire.start();
    LOG.debug("KmsAwsRule started");
  }

  @Override
  protected void after() {
    wire.stop();
    LOG.debug("KmsAwsRule shut down");
  }

  @Override
  public Statement apply(Statement base, Description description) {
    return RuleChain.outerRule(wire).apply(base, description);
  }

  public String getEndpointUrl() {
    return wire.baseUrl();
  }

  private void initWireMocks() throws JsonProcessingException {
    // mock "GenerateDataKey"
    wire.stubFor(
        post("/")
            .withHeader(CONTENT_TYPE, equalTo(MEDIA_TYPE_AWS))
            .withHeader("x-amz-target", equalTo("TrentService.GenerateDataKey"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(CONTENT_TYPE, MEDIA_TYPE_AWS)
                    .withBody(OBJECT_MAPPER.writeValueAsString(createGenerateDataKeyResult()))
                    .withTransformers("response-template")));

    // mock "Decrypt" data key
    wire.stubFor(
        post("/")
            .withHeader(CONTENT_TYPE, equalTo(MEDIA_TYPE_AWS))
            .withHeader("x-amz-target", equalTo("TrentService.Decrypt"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(CONTENT_TYPE, MEDIA_TYPE_AWS)
                    .withBody(OBJECT_MAPPER.writeValueAsString(createDecryptResult()))
                    .withTransformers("response-template")));
  }

  private ExtendedDecryptResult createDecryptResult() {
    ExtendedDecryptResult result = new ExtendedDecryptResult();
    result.setEncryptionAlgorithm("SYMMETRIC_DEFAULT");
    result.setKeyId("{{request.body.KeyId}}");
    result.setPlaintext(ByteBuffer.wrap(PK_PLAIN_TEXT));
    return result;
  }

  private ExtendedGenerateDataKeyResult createGenerateDataKeyResult() {
    ExtendedGenerateDataKeyResult result = new ExtendedGenerateDataKeyResult();
    result.setKeyId("{{request.body.KeyId}}");
    result.setPlaintext(ByteBuffer.wrap(PK_PLAIN_TEXT));
    result.setCiphertextBlob(ByteBuffer.wrap(CIPHER_TEXT_BLOB));
    return result;
  }
}
