package org.sdase.commons.shared.yaml;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

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

   @Test
   public void testReadListOfBean2FromInputStream() throws IOException {

      try (InputStream resourceAsStream = getClass().getResourceAsStream("/datasets/test-list.yaml")) { // NOSONAR
         List<TestBean2> actual = YamlUtil.load(
               resourceAsStream,
               new TypeReference<List<TestBean2>>() {
               }
         );

         assertThat(actual)
               .extracting(TestBean2::getId, TestBean2::getMessage, TestBean2::getAttribute)
               .containsExactly(
                     tuple("123", "Hello World!", "Foo"), // NOSONAR
                     tuple("456", "Hello Universe!", "Bar") // NOSONAR
               );
      }
   }

   @Test
   public void testReadListOfBean2FromStringContent() throws IOException {

      try (InputStream resourceAsStream = getClass().getResourceAsStream("/datasets/test-list.yaml")) {
         String content = IOUtils.toString(resourceAsStream, UTF_8);

         List<TestBean2> actual = YamlUtil.load(content, new TypeReference<List<TestBean2>>() {
         });

         assertThat(actual)
               .extracting(TestBean2::getId, TestBean2::getMessage, TestBean2::getAttribute)
               .containsExactly(
                     tuple("123", "Hello World!", "Foo"),
                     tuple("456", "Hello Universe!", "Bar")
               );
      }
   }

   @Test
   public void testReadListOfBean2FromStringUri() {

      List<TestBean2> actual = YamlUtil.load(
            getClass().getResource("/datasets/test-list.yaml"),
            new TypeReference<List<TestBean2>>() {});

      assertThat(actual)
            .extracting(TestBean2::getId, TestBean2::getMessage, TestBean2::getAttribute)
            .containsExactly(
                  tuple("123", "Hello World!", "Foo"),
                  tuple("456", "Hello Universe!", "Bar")
            );
   }

   @Test
   public void testLoadZonedDateTimeUtc() {
      TimeAwareBean actual = YamlUtil.load("time: 2019-02-18T11:06:11.634310066Z", TimeAwareBean.class);

      assertThat(actual.getTime()).isEqualTo("2019-02-18T11:06:11.634310066Z");
   }

   @Test
   public void testLoadZonedDateTimeBerlin() {
      TimeAwareBean actual = YamlUtil.load("time: 2019-02-18T11:06:11.634310066+01:00", TimeAwareBean.class);

      assertThat(actual.getTime()).isEqualTo("2019-02-18T10:06:11.634310066Z");
   }

   @Test
   public void testLoadYamlWithMultipleFilesURLClass() {
      List<TestBean2> actual = YamlUtil.loadList(
          getClass().getResource("/datasets/test-multiple-files.yml"),
          TestBean2.class);

      assertThat(actual)
          .extracting(TestBean2::getId, TestBean2::getMessage, TestBean2::getAttribute)
          .containsExactly(
              tuple("123", "Hello World!", "Foo"),
              tuple("456", "Hello Universe!", "Bar")
          );
   }

   @Test
   public void testLoadYamlWithMultipleFilesURLTypeReference() {
      List<TestBean2> actual = YamlUtil.loadList(
          getClass().getResource("/datasets/test-multiple-files.yml"),
          new TypeReference<TestBean2>() {
          });

      assertThat(actual)
          .extracting(TestBean2::getId, TestBean2::getMessage, TestBean2::getAttribute)
          .containsExactly(
              tuple("123", "Hello World!", "Foo"),
              tuple("456", "Hello Universe!", "Bar")
          );
   }

   @Test
   public void testLoadYamlWithMultipleFilesInputStreamClass() throws IOException {
      try (InputStream resourceAsStream = getClass().getResourceAsStream("/datasets/test-multiple-files.yml")) { // NOSONAR
         List<TestBean2> actual = YamlUtil.loadList(resourceAsStream, TestBean2.class);

         assertThat(actual)
             .extracting(TestBean2::getId, TestBean2::getMessage, TestBean2::getAttribute)
             .containsExactly(
                 tuple("123", "Hello World!", "Foo"),
                 tuple("456", "Hello Universe!", "Bar")
             );
      }
   }

   @Test
   public void testLoadYamlWithMultipleFilesInputStreamTypeReference() throws IOException {
      try (InputStream resourceAsStream = getClass().getResourceAsStream("/datasets/test-multiple-files.yml")) { // NOSONAR
         List<TestBean2> actual = YamlUtil.loadList(resourceAsStream, new TypeReference<TestBean2>() {});

         assertThat(actual)
             .extracting(TestBean2::getId, TestBean2::getMessage, TestBean2::getAttribute)
             .containsExactly(
                 tuple("123", "Hello World!", "Foo"),
                 tuple("456", "Hello Universe!", "Bar")
             );
      }
   }

   @Test
   public void testLoadYamlWithMultipleFilesStringClass() throws IOException {
      try (InputStream resourceAsStream = getClass().getResourceAsStream("/datasets/test-multiple-files.yml")) { // NOSONAR
         String content = IOUtils.toString(resourceAsStream, UTF_8);
         List<TestBean2> actual = YamlUtil.loadList(content, TestBean2.class);

         assertThat(actual)
             .extracting(TestBean2::getId, TestBean2::getMessage, TestBean2::getAttribute)
             .containsExactly(
                 tuple("123", "Hello World!", "Foo"),
                 tuple("456", "Hello Universe!", "Bar")
             );
      }
   }

   @Test
   public void testLoadYamlWithMultipleFilesStringTypeReference() throws IOException {
      try (InputStream resourceAsStream = getClass().getResourceAsStream("/datasets/test-multiple-files.yml")) { // NOSONAR
         String content = IOUtils.toString(resourceAsStream, UTF_8);
         List<TestBean2> actual = YamlUtil.loadList(content, new TypeReference<TestBean2>() {});

         assertThat(actual)
             .extracting(TestBean2::getId, TestBean2::getMessage, TestBean2::getAttribute)
             .containsExactly(
                 tuple("123", "Hello World!", "Foo"),
                 tuple("456", "Hello Universe!", "Bar")
             );
      }
   }
}
