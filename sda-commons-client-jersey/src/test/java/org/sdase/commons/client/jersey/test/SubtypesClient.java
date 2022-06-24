package org.sdase.commons.client.jersey.test;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * This client shows an example, how the response body of a GET request may be validated.
 */
@Produces(MediaType.APPLICATION_JSON)
@Path("/subtypes")
public interface SubtypesClient {

  Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

  @GET
  @Path("")
  AbstractResource getSubtype();

  /**
   * @return the response object for the request of {@link #getSubtype()}
   * @throws IllegalArgumentException if the received object maps to {@code null}, e.g. due to
   *     deserialization errors
   * @throws ConstraintViolationException if the received object is invalid regarding the defined
   *     constraints in {@link AbstractResource} and it's implementations
   */
  default AbstractResource getOnlyValidSubtype()
      throws IllegalArgumentException, ConstraintViolationException {
    AbstractResource resource = getSubtype();
    Set<ConstraintViolation<AbstractResource>> validate = VALIDATOR.validate(resource);
    if (!validate.isEmpty()) {
      throw new ConstraintViolationException(validate);
    }
    return resource;
  }

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", visible = true)
  @JsonSubTypes({
    @JsonSubTypes.Type(value = ResourceOne.class, name = "ONE"),
    @JsonSubTypes.Type(value = ResourceTwo.class, name = "TWO")
  })
  abstract class AbstractResource {

    private ResourceType type;

    public ResourceType getType() {
      return type;
    }

    public AbstractResource setType(ResourceType type) {
      this.type = type;
      return this;
    }

    public enum ResourceType {
      ONE,
      TWO
    }
  }

  class ResourceOne extends AbstractResource {}

  class ResourceTwo extends AbstractResource {

    @NotNull private AbstractResource nested;

    @NotNull private ResourceType anyType;

    public AbstractResource getNested() {
      return nested;
    }

    public ResourceTwo setNested(AbstractResource nested) {
      this.nested = nested;
      return this;
    }

    public ResourceType getAnyType() {
      return anyType;
    }

    public ResourceTwo setAnyType(ResourceType anyType) {
      this.anyType = anyType;
      return this;
    }
  }
}
