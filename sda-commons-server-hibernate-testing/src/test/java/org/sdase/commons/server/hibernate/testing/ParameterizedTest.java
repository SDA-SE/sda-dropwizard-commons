package org.sdase.commons.server.hibernate.testing;

import com.github.database.rider.core.DBUnitRule;
import com.github.database.rider.core.api.configuration.DBUnit;
import java.util.Arrays;
import java.util.Collection;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * This test ensures that the used rider version is able to run parameterized tests. Versions 1.3.0
 * and 1.4.0 fail when using the {@link com.github.database.rider.core.DBUnitRule}
 */
@RunWith(Parameterized.class)
@DBUnit(url = "jdbc:h2:mem:test", driver = "org.h2.Driver", user = "sa", password = "sa")
public class ParameterizedTest {

  private int given;
  private int expected;

  @Rule public final DBUnitRule dbUnitRule = DBUnitRule.instance();

  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[] {0, 0}, new Object[] {1, 1}, new Object[] {2, 2});
  }

  public ParameterizedTest(int given, int expected) {
    this.given = given;
    this.expected = expected;
  }

  @Test
  public void shouldJustRunMoreThanOneTime() {
    Assertions.assertThat(given).isEqualTo(expected);
  }
}
