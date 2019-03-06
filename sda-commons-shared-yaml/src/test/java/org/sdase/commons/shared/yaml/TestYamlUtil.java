package org.sdase.commons.shared.yaml;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.net.URL;
import org.junit.Test;

public class TestYamlUtil {

   @Test
   public void testLoadYaml() {
      // given
      URL resource = this.getClass().getClassLoader().getResource("datasets/testbean2.yml");

      // when
      TestBean2 tb = YamlUtil.load(resource, TestBean2.class);

      // then
      assertThat(tb).isNotNull();
      assertThat(tb.getAttribute()).isEqualTo("attribute1"); // NOSONAR
      assertThat(tb.getMessage()).isEqualTo("Hello"); // NOSONAR
   }

   @Test
   public void testLoadYamlTolerantReader() {
      // given
      URL resource = this.getClass().getClassLoader().getResource("datasets/testbean3.yml");

      // when
      TestBean2 tb = YamlUtil.load(resource, TestBean2.class);

      // then
      assertThat(tb).isNotNull();
      assertThat(tb.getAttribute()).isEqualTo("attribute1");
      assertThat(tb.getMessage()).isEqualTo("Hello");
   }

   @Test
   public void testLoadYamlWithMemberUsingInputStream() {
      // given
      InputStream resource = this.getClass().getClassLoader().getResourceAsStream("datasets/testbean1.yml");

      // when
      TestBean1 tb = YamlUtil.load(resource, TestBean1.class);

      // then
      assertThat(tb).isNotNull();
      assertThat(tb.getBean()).isNotNull();
      assertThat(tb.getBean().getAttribute()).isEqualTo("attribute1");
      assertThat(tb.getBean().getMessage()).isEqualTo("Hello");
   }

   @Test
   public void testLoadYamlWithMemberUsingString() {
      // given
      String resource = "---\nmessage: \"Hello\"\nattribute: \"attribute1\"\n";

      // when
      TestBean2 tb = YamlUtil.load(resource, TestBean2.class);

      // then
      assertThat(tb).isNotNull();
      assertThat(tb.getAttribute()).isEqualTo("attribute1");
      assertThat(tb.getMessage()).isEqualTo("Hello");
   }

   @Test
   public void testWriteValueAsString() {
      // given
      TestBean2 tb = new TestBean2();
      tb.setAttribute("attribute1");
      tb.setMessage("Hello");

      // when
      String actual = YamlUtil.writeValueAsString(tb);

      // then
      assertThat(actual).isEqualTo("---\nmessage: \"Hello\"\nattribute: \"attribute1\"\nid: null\n");
   }
}
