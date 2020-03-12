package org.sdase.commons.server.testing;

import java.util.function.Supplier;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * This {@link TestRule} allows to wrap another rule to defer the initialization of the rule till
 * the rule is started the first time. This allows to initialize a rule with parameters that are
 * only available once another rule is completely initialized. This is often required if one rule
 * opens a random port that the other rule want to connect to.
 *
 * <p>The wrapped rule can be accessed via {@link LazyRule#getRule()}
 *
 * <p>Example:
 *
 * <pre>
 *   class MyTest {
 *     private static final WireMockClassRule WIRE =
 *         new WireMockClassRule(wireMockConfig().dynamicPort());
 *     private static final LazyRule&#60;DropwizardAppRule&#60;AppConfiguration&#62;&#62; DW =
 *         new LazyRule&#60;&#62;(
 *             () -&#62;
 *                 new DropwizardAppRule&#60;&#62;(
 *                     TestApplication.class,
 *                     ResourceHelpers.resourceFilePath("test-config.yml"),
 *                     ConfigOverride.config("url", WIRE.baseUrl())));
 *
 *     &#64;ClassRule public static final RuleChain CHAIN = RuleChain.outerRule(WIRE).around(DW);
 *   }
 * </pre>
 */
public class LazyRule<T extends TestRule> implements TestRule {
  private Supplier<T> ruleSupplier;
  private T rule;

  public LazyRule(Supplier<T> ruleSupplier) {
    this.ruleSupplier = ruleSupplier;
  }

  public Statement apply(Statement base, Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        rule = ruleSupplier.get();
        Statement statement = rule.apply(base, description);
        statement.evaluate();
      }
    };
  }

  /**
   * Provides access to the wrapped rule. Throws if the rule isn't initialized yet.
   *
   * @return The wrapped {@link TestRule}.
   */
  public T getRule() {
    if (rule == null) {
      throw new IllegalStateException("rule not yet initialized");
    }

    return rule;
  }
}
