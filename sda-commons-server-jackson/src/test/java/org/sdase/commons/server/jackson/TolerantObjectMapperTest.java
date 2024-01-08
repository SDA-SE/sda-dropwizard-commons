package org.sdase.commons.server.jackson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.core.Application;
import io.dropwizard.core.Configuration;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TolerantObjectMapperTest {

  private ObjectMapper om;

  @BeforeEach
  void setUp() {
    this.om = ObjectMapperConfigurationUtil.configureMapper().build();
  }

  @Test
  void omShouldHaveSameModulesAsDefaultFromDropwizardButNoFuzzyEnumAndBlackbirdModules() {
    Bootstrap<Configuration> standardBootstrap =
        new Bootstrap<>(
            new Application<Configuration>() {
              @Override
              public void run(Configuration configuration, Environment environment) {}
            });
    ObjectMapper dropwizardStandardOm = standardBootstrap.getObjectMapper();
    String unwantedFuzzyEnumModule = "io.dropwizard.jackson.FuzzyEnumModule";
    String unwantedBlackbirdModule = "com.fasterxml.jackson.module.blackbird.BlackbirdModule";

    Set<Object> expected = new HashSet<>(dropwizardStandardOm.getRegisteredModuleIds());

    // if this fails (due to Dropwizard upgrade) we may go back to
    // io.dropwizard.jackson.Jackson.newObjectMapper() or the default
    // ObjectMapper provided by Bootstrap instead of customizing the ObjectMapper
    // instantiation in JacksonConfigurationBundle#initialize(Bootstrap)
    assertThat(expected).contains(unwantedFuzzyEnumModule, unwantedBlackbirdModule);

    expected.remove(unwantedFuzzyEnumModule);
    expected.remove(unwantedBlackbirdModule);

    assertThat(om.getRegisteredModuleIds())
        .containsAll(expected)
        .doesNotContain(unwantedFuzzyEnumModule);
  }

  @Test
  void deserializeJsonWithUnknownFields() throws Exception {
    // age is not part of model class
    String given = "{\"name\": \"John Doe\", \"age\": 28}";

    Person actual = om.readValue(given, Person.class);

    assertThat(actual)
        .extracting(Person::getName, Person::getDob)
        .containsExactly("John Doe", null);
  }

  @Test
  void readSingleStringAsList() throws Exception {
    String given = "{\"addresses\": \"Main Street 1\\n12345 Gotham City\"}";

    Person actual = om.readValue(given, Person.class);

    assertThat(actual.getAddresses()).containsExactly("Main Street 1\n12345 Gotham City");
  }

  @Test
  void readEnumValue() throws Exception {
    String given = "{\"title\": \"DOCTOR\"}";

    Person actual = om.readValue(given, Person.class);

    assertThat(actual).extracting(Person::getTitle).isEqualTo(Title.DOCTOR);
  }

  @Test
  void readUnknownEnumAsNull() throws Exception {
    String given = "{\"title\": \"DOCTOR_HC\"}";

    Person actual = om.readValue(given, Person.class);

    assertThat(actual).extracting(Person::getTitle).isNull();
  }

  @Test
  void readEnumValueWithDefault() throws Exception {
    String given = "{\"profession\": \"IT\"}";

    Person actual = om.readValue(given, Person.class);

    assertThat(actual).extracting(Person::getProfession).isEqualTo(Profession.IT);
  }

  @Test
  void readUnknownEnumValueAsDefault() throws Exception {
    String given = "{\"profession\": \"CRAFTMANSHIP\"}";

    Person actual = om.readValue(given, Person.class);

    assertThat(actual).extracting(Person::getProfession).isEqualTo(Profession.OTHER);
  }

  @Test
  void writeEmptyBeans() throws Exception {
    String actual = om.writeValueAsString(new Object());

    assertThat(actual).isEqualTo("{}");
  }

  @Test
  void failOnSelfReferenceToAvoidRecursion() {
    Person given = new Person();
    given.setPartner(given);

    assertThatExceptionOfType(JsonMappingException.class)
        .isThrownBy(() -> om.writeValueAsString(given))
        .withMessageContaining("cycle");
  }

  @Test
  void writeNullFields() throws Exception {

    Person given = new Person();

    String actual = om.writeValueAsString(given);

    assertThat(actual)
        .contains("\"name\":null")
        .contains("\"title\":null")
        .contains("\"dob\":null")
        .contains("\"addresses\":null")
        .contains("\"partner\":null")
        .contains("\"profession\":null");
  }

  @Test
  void doNotWriteIgnoredField() throws Exception {

    Person given = new Person().setIdCardNumber("123-456-789");

    String actual = om.writeValueAsString(given);

    assertThat(actual).doesNotContain("idCardNumber", "123-456-789");
  }

  @Test
  void skipIgnoredFieldWhenReading() throws Exception {

    String given = "{\"idCardNumber\": \"123-456-789\"}";

    Person actual = om.readValue(given, Person.class);

    assertThat(actual).isNotNull().extracting(Person::getIdCardNumber).isNull();
  }

  @Test
  void shouldReadSubType() throws Exception {
    String given = "{\"type\":\"my\", \"value\":\"foo\"}";

    Filter actual = om.readValue(given, Filter.class);

    assertThat(actual).isInstanceOf(MyFilter.class).extracting("value").isEqualTo("foo");
  }

  @Test
  void shouldNotFailForUnknownSubtype() throws Exception {
    String given = "{\"type\":\"notMy\", \"value\":\"foo\"}";

    Filter actual = om.readValue(given, Filter.class);

    assertThat(actual).isNull();
  }

  @SuppressWarnings({"WeakerAccess", "unused", "UnusedReturnValue"})
  private static class Person {

    private String name;
    private Title title;
    private LocalDate dob;
    private List<String> addresses;
    private Person partner;
    @JsonIgnore private String idCardNumber;

    private Profession profession;

    public String getName() {
      return name;
    }

    public Person setName(String name) {
      this.name = name;
      return this;
    }

    public Title getTitle() {
      return title;
    }

    public Person setTitle(Title title) {
      this.title = title;
      return this;
    }

    public LocalDate getDob() {
      return dob;
    }

    public Person setDob(LocalDate dob) {
      this.dob = dob;
      return this;
    }

    public List<String> getAddresses() {
      return addresses;
    }

    public Person setAddresses(List<String> addresses) {
      this.addresses = addresses;
      return this;
    }

    public Person getPartner() {
      return partner;
    }

    public Person setPartner(Person partner) {
      this.partner = partner;
      return this;
    }

    public String getIdCardNumber() {
      return idCardNumber;
    }

    public Person setIdCardNumber(String idCardNumber) {
      this.idCardNumber = idCardNumber;
      return this;
    }

    public Profession getProfession() {
      return profession;
    }

    public Person setProfession(Profession profession) {
      this.profession = profession;
      return this;
    }
  }

  @SuppressWarnings("unused")
  private enum Title {
    PROFESSOR,
    DOCTOR
  }

  @SuppressWarnings("unused")
  private enum Profession {
    IT,
    FINANCE,
    LEGAL,
    @JsonEnumDefaultValue
    OTHER
  }

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", visible = true)
  @JsonSubTypes({@JsonSubTypes.Type(value = MyFilter.class, name = "my")})
  private interface Filter {}

  @SuppressWarnings("unused")
  private static class MyFilter implements Filter {
    private String value;

    public MyFilter setValue(String value) {
      this.value = value;
      return this;
    }

    public String getValue() {
      return value;
    }
  }
}
