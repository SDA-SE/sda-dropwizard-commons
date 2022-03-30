package org.sdase.commons.server.cloudevents;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;
import org.sdase.commons.server.cloudevents.app.consume.ContractCreatedEvent;
import org.sdase.commons.server.cloudevents.app.produce.PartnerCreatedEvent;
import org.sdase.commons.server.testing.GoldenFileAssertions;
import org.sdase.commons.shared.asyncapi.AsyncApiGenerator;

public class AsyncApiDocumentationTest {
  @Test
  public void generateAndVerifySpec() throws IOException {
    String expected =
        AsyncApiGenerator.builder()
            .withAsyncApiBase(getClass().getResource("/asyncapi-template.yml"))
            .withSchema("/PartnerCreatedEvent", PartnerCreatedEvent.class)
            .withSchema("/ContractCreatedEvent", ContractCreatedEvent.class)
            .withSchema("/PlainCloudEvent", CloudEventV1.class)
            .generateYaml();

    // specify where you want your file to be stored
    Path filePath = Paths.get("asyncapi.yaml");

    // check and update the file
    GoldenFileAssertions.assertThat(filePath).hasContentAndUpdateGolden(expected);
  }
}
