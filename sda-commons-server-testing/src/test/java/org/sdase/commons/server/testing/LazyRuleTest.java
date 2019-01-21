package org.sdase.commons.server.testing;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.sdase.commons.server.testing.test.ClientRule;
import org.sdase.commons.server.testing.test.ServerRule;

public class LazyRuleTest {

   private static final ServerRule SERVER = new ServerRule();

   private static final LazyRule<ClientRule> CLIENT = new LazyRule<>(() -> new ClientRule(SERVER.getPort()));

   @ClassRule
   public static final RuleChain RULE_CHAIN = RuleChain.outerRule(SERVER).around(CLIENT);

   @Test
   public void testLazyInitialization() {
      assertThat(CLIENT.getRule().getPort()).isEqualTo(4);
   }

}
