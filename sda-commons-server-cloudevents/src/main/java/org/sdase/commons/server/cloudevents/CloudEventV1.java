package org.sdase.commons.server.cloudevents;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaDefault;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaExamples;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * @param <T> the type of the data element of this CloudEvent
 * @see <a href="https://github.com/cloudevents/spec/blob/v1.0.2/cloudevents/spec.md">1.0.2
 *     Specification</a>
 */
@JsonClassDescription(
    "This is a manifestation of the [CloudEvents](https://github.com/cloudevents/spec/blob/v1.0.2/cloudevents/spec.md) "
        + "specification. The field documentation will contain the official documentation and also "
        + "some hints how values should should be set when used on the SDA platform.")
public class CloudEventV1<T> extends BaseCloudEvent {

  @JsonPropertyDescription(
      "Identifies the event. Producers MUST ensure that `source` + `id` is "
          + "unique for each distinct event. If a duplicate event is re-sent (e.g. due to a "
          + "network error) it MAY have the same id. Consumers MAY assume that Events with "
          + "identical `source` and `id` are duplicates.\n"
          + "\n"
          + "**SDA**: The default is to use a random UUID.")
  @JsonSchemaExamples("57d67827-3f4f-46e8-a126-fa6a6b724ae2")
  @NotEmpty
  private String id = UUID.randomUUID().toString();

  @JsonPropertyDescription(
      "Identifies the context in which an event happened. Often this will "
          + "include information such as the type of the event source, the organization publishing the "
          + "event or the process that produced the event. The exact syntax and semantics behind the "
          + "data encoded in the URI is defined by the event producer.\n"
          + "\n"
          + "Producers MUST ensure that `source` + `id` is unique for each distinct event.\n"
          + "\n"
          + "An application MAY assign a unique `source` to each distinct producer, which makes it "
          + "easy to produce unique IDs since no other producer will have the same source. The "
          + "application MAY use UUIDs, URNs, DNS authorities or an application-specific scheme to "
          + "create unique source identifiers.\n"
          + "\n"
          + "A source MAY include more than one producer. In that case the producers MUST collaborate "
          + "to ensure that `source` + `id` is unique for each distinct event.\n"
          + "\n"
          + "**SDA**: Most importantly the source MUST identify the producer of your event "
          + "uniquely. If you don't have any other guidelines defined by your company we recommend "
          + "the following pattern:\n"
          + "`/COMPANY/DOMAIN/SYSTEM/SERVICE`\n"
          + "\n"
          + "- `COMPANY`: An identifier of the company that is responsible for the service, "
          + "e.g. `SDA-SE`\n"
          + "- `DOMAIN`: The name of the business domain of the service, e.g. `consent`\n"
          + "- `SYSTEM`: The name of the system of the service, e.g. `partner-consent-stack`\n"
          + "- `SERVICE`: The name of the service, e.g. `consent-configuration-service`")
  @JsonSchemaExamples({
    "/COMPANY/DOMAIN/SYSTEM/SERVICE",
    "/SDA-SE/consent/partner-consent-stack/consent-configuration-service"
  })
  @NotNull
  private URI source;

  /**
   * @see <a
   *     href="https://github.com/cloudevents/spec/blob/v1.0/primer.md#versioning-of-attributes">Versioning
   *     of Attributes in the Primer</a>
   */
  @JsonPropertyDescription(
      "This attribute contains a value describing the type of event related "
          + "to the originating occurrence. Often this attribute is used for routing, observability, "
          + "policy enforcement, etc. The format of this is producer defined and might include "
          + "information such as the version of the type - see Versioning of Attributes in the Primer "
          + "for more information.\n"
          + "\n"
          + "**SDA**: If you don't have any other guidelines defined by your company we recommend \n"
          + "the following pattern:\n"
          + "`DOMAIN_ELEMENT_OPERATION`, e.g. `CONSENT_VERSION_CREATED`\n"
          + "\n"
          + "⚠️ Please be careful if you want to use the classname of your event as `type` because "
          + "it makes it harder to refactor/rename your class. We definitely do not recommend to "
          + "use the fully qualified classname that includes the package.")
  @JsonSchemaExamples("CONSENT_VERSION_CREATED")
  @NotEmpty
  private String type;

  @JsonPropertyDescription(
      "This describes the subject of the event in the context of the event "
          + "producer (identified by `source`). In publish-subscribe scenarios, a subscriber will "
          + "typically subscribe to events emitted by a source, but the `source` identifier alone might "
          + "not be sufficient as a qualifier for any specific event if the `source` context has "
          + "internal sub-structure.\n"
          + "\n"
          + "Identifying the subject of the event in context metadata (opposed to only in the data "
          + "payload) is particularly helpful in generic subscription filtering scenarios where "
          + "middleware is unable to interpret the data content. In the above example, the subscriber "
          + "might only be interested in blobs with names ending with '.jpg' or '.jpeg' and the "
          + "subject attribute allows for constructing a simple and efficient string-suffix filter "
          + "for that subset of events.")
  @JsonSchemaExamples("terms-and-conditions-1")
  private String subject;

  @JsonPropertyDescription(
      "Content type of data value. This attribute enables data to carry any "
          + "type of content, whereby format and encoding might differ from that of the chosen event "
          + "format. For example, an event rendered using the JSON envelope format might carry an XML "
          + "payload in data, and the consumer is informed by this attribute being set to "
          + "\"application/xml\". The rules for how data content is rendered for different "
          + "`datacontenttype` values are defined in the event format specifications; for example, the "
          + "JSON event format defines the relationship in section 3.1.\n"
          + "\n"
          + "For some binary mode protocol bindings, this field is directly mapped to the respective "
          + "protocol's content-type metadata property. Normative rules for the binary mode and the "
          + "content-type metadata mapping can be found in the respective protocol\n"
          + "\n"
          + "In some event formats the `datacontenttype` attribute MAY be omitted. For example, if a "
          + "JSON format event has no `datacontenttype` attribute, then it is implied that the data is "
          + "a JSON value conforming to the \"application/json\" media type. In other words: a "
          + "JSON-format event with no `datacontenttype` is exactly equivalent to one with "
          + "`datacontenttype=\"application/json\"`.\n"
          + "\n"
          + "When translating an event message with no `datacontenttype` attribute to a different format "
          + "or protocol binding, the target `datacontenttype` SHOULD be set explicitly to the implied "
          + "`datacontenttype` of the source.\n"
          + "\n"
          + "**SDA**: The default is to use 'application/json'")
  @JsonSchemaExamples("application/json")
  @JsonSchemaDefault("application/json")
  private String datacontenttype = "application/json";

  @JsonPropertyDescription(
      "Timestamp of when the occurrence happened. If the time of the "
          + "occurrence cannot be determined then this attribute MAY be set to some other time (such "
          + "as the current time) by the CloudEvents producer, however all producers for the same "
          + "source MUST be consistent in this respect. In other words, either they all use the actual "
          + "time of the occurrence or they all use the same algorithm to determine the value "
          + "used.\n"
          + "\n"
          + "**SDA**: Default will be set to the current time.")
  @JsonSchemaExamples("2022-03-12T23:20:50.52Z")
  private OffsetDateTime time = OffsetDateTime.now();

  @JsonPropertyDescription(
      "As defined by the term Data, CloudEvents MAY include domain-specific "
          + "information about the occurrence.")
  private T data;

  public CloudEventV1() {
    this.setSpecversion("1.0");
  }

  public CloudEventV1(String id, T data) {
    this();
    this.id = id;
    this.data = data;
  }

  public String getId() {
    return id;
  }

  public CloudEventV1<T> setId(String id) {
    this.id = id;
    return this;
  }

  public URI getSource() {
    return source;
  }

  public CloudEventV1<T> setSource(URI source) {
    this.source = source;
    return this;
  }

  public String getType() {
    return type;
  }

  public CloudEventV1<T> setType(String type) {
    this.type = type;
    return this;
  }

  public String getSubject() {
    return subject;
  }

  public CloudEventV1<T> setSubject(String subject) {
    this.subject = subject;
    return this;
  }

  public String getDatacontenttype() {
    return datacontenttype;
  }

  public CloudEventV1<T> setDatacontenttype(String datacontenttype) {
    this.datacontenttype = datacontenttype;
    return this;
  }

  public OffsetDateTime getTime() {
    return time;
  }

  public CloudEventV1<T> setTime(OffsetDateTime time) {
    this.time = time;
    return this;
  }

  public T getData() {
    return data;
  }

  public CloudEventV1<T> setData(T data) {
    this.data = data;
    return this;
  }
}
