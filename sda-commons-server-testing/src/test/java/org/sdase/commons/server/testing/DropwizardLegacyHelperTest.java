package org.sdase.commons.server.testing;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import javax.ws.rs.client.Client;
import org.junit.jupiter.api.Test;

class DropwizardLegacyHelperTest {

  @Test
  void shouldProvideClient() {
    Client client = DropwizardLegacyHelper.client(new ObjectMapper());
    assertThat(client).isNotNull();
  }
}
