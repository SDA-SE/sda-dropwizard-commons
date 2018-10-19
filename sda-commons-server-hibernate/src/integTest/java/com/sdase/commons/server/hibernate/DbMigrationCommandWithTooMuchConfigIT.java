package com.sdase.commons.server.hibernate;

import io.dropwizard.Application;
import io.dropwizard.cli.Cli;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DAOTestRule;
import io.dropwizard.util.JarLocation;
import org.hibernate.cfg.AvailableSettings;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DbMigrationCommandWithTooMuchConfigIT {

   private static final String DB_URL = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1";

   private final PrintStream originalOut = System.out;
   private final PrintStream originalErr = System.err;
   private final InputStream originalIn = System.in;

   private final ByteArrayOutputStream stdOut = new ByteArrayOutputStream();
   private final ByteArrayOutputStream stdErr = new ByteArrayOutputStream();
   private Cli cli;

   @Rule
   public final DAOTestRule daoTestRule = DAOTestRule
         .newBuilder()
         .setProperty(AvailableSettings.URL, DB_URL)
         .setProperty(AvailableSettings.USER, "sa")
         .setProperty(AvailableSettings.PASS, "sa")
         .setProperty(AvailableSettings.DEFAULT_SCHEMA, "PUBLIC")
         .build();

   @Before
   public void setUp() {
      // Setup necessary mock
      JarLocation location = mock(JarLocation.class);
      when(location.getVersion()).thenReturn(Optional.of("1.0.0"));

      // Add commands you want to test
      Bootstrap<DbMigrationConfiguration> bootstrap = new Bootstrap<>(new Application<DbMigrationConfiguration>() {
         @Override
         public void run(DbMigrationConfiguration configuration, Environment environment) {

         }
      });
      bootstrap.addCommand(new DbMigrationCommand());

      // Redirect stdout and stderr to our byte streams
      System.setOut(new PrintStream(stdOut));
      System.setErr(new PrintStream(stdErr));

      // Build what'll run the command and interpret arguments
      cli = new Cli(location, bootstrap, stdOut, stdErr);

   }

   @Test
   public void shouldExecuteWithConfigThatHasUnknownProperties() throws Exception {
      boolean migrateDB = cli.run("migrateDB", ResourceHelpers.resourceFilePath("test-config-too-much-information.yaml"));

      assertThat(migrateDB).as("Exit success").isTrue();
      assertThat(stdOut.toString())
            .doesNotContain("io.dropwizard.configuration.ConfigurationParsingException")
            .contains("Database migration successful");
      assertThat(stdErr.toString()).isEmpty();

   }

   @After
   public void resetStreams() {
      System.setOut(originalOut);
      System.setErr(originalErr);
      System.setIn(originalIn);
      // append the output we have hidden to test the result of the cli command
      System.out.append(stdOut.toString());
      System.err.append(stdErr.toString());
   }

}
