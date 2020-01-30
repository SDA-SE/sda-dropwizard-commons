package org.sdase.commons.server.morphia.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.codahale.metrics.health.HealthCheck;
import com.mongodb.MongoExecutionTimeoutException;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Tests that the health checks reports healthy when the ping result is ok and unhealthy if
 * exception is thrown
 */
public class HealthCheckTest {

  private MongoDatabase dbMock;

  @Before
  public void setUp() {
    dbMock = mock(MongoDatabase.class, Mockito.RETURNS_DEEP_STUBS);
  }

  @Test
  public void shouldBeHealthy() {
    // given
    when(dbMock.runCommand(Mockito.any())).thenReturn(new Document("ok", Double.valueOf("1.0")));

    // when
    HealthCheck.Result result = new MongoHealthCheck(dbMock).check();

    // then
    assertThat(result.isHealthy()).isTrue();
  }

  @Test
  public void shouldBeUnHealthy() {
    // given
    when(dbMock.runCommand(Mockito.any()))
        .thenThrow(new MongoExecutionTimeoutException(1, "DUMMY"));

    // when
    HealthCheck.Result result = new MongoHealthCheck(dbMock).check();

    // then
    assertThat(result.isHealthy()).isFalse();
  }
}
