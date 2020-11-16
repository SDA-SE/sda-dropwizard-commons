package org.sdase.commons.starter.example.people.rest;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import io.openapitools.jackson.dataformat.hal.HALLink;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.security.PermitAll;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import org.sdase.commons.server.dropwizard.ContextAwareEndpoint;
import org.sdase.commons.starter.example.people.db.PersonEntity;
import org.sdase.commons.starter.example.people.db.PersonManager;

/** Example endpoint for getting information about people. */
@Path("people") // define the base path of this endpoint
@Produces({
  APPLICATION_JSON,
  "application/hal+json"
}) // should be set to produce 406 for other accept headers
@Consumes({APPLICATION_JSON}) // should be set to produce 406 for other accept headers
@PermitAll // Require authentication for this endpoint
public class PersonEndpoint implements ContextAwareEndpoint {

  /**
   * Information about the requested URI. Jersey will inject a Proxy of {@code UriInfo} that is
   * aware of the request scope.
   */
  @Context private UriInfo uriInfo;

  private PersonManager personManager;

  public PersonEndpoint(PersonManager personManager) {
    this.personManager = personManager;
  }

  @GET // maps to "GET /people"
  public List<PersonResource> findAllPeople() {
    return personManager.findAll().stream().map(this::toResource).collect(Collectors.toList());
  }

  @GET
  @Path("{personId}") // maps to "GET /people/{personId}, e.g. "GET /people/john-doe"
  public PersonResource findPersonById(@PathParam("personId") String personId) {
    PersonEntity personEntity = personManager.findById(personId);
    if (personEntity == null) {
      throw new NotFoundException();
    }
    return toResource(personEntity);
  }

  /**
   * Creates a {@link PersonResource} representing the given {@link PersonEntity}. We do not want to
   * expose the underlying entities to our consumers. This way our API is independent from the
   * internal model which may be changed with an appropriate migration at any time. Especially when
   * using HAL resources, we have the {@code _links} as dynamic content that is not persisted with
   * our entity.
   *
   * @param personEntity the entity that should be exposed as {@link PersonResource}
   * @return the {@link PersonResource} containing links to the {@linkplain
   *     PersonEntity#getChildren() children} and {@linkplain PersonEntity#getParents() parents} of
   *     the {@code personEntity} instead of embedding all the data.
   */
  private PersonResource toResource(PersonEntity personEntity) {
    String id = personEntity.getId();
    PersonResource personResource =
        new PersonResource()
            .setFirstName(personEntity.getFirstName())
            .setLastName(personEntity.getLastName())
            .setSelfLink(new HALLink.Builder(personLocation(id)).build());
    if (personEntity.getChildren() != null && !personEntity.getChildren().isEmpty()) {
      personResource.setChildrenLinks(
          personEntity.getChildren().stream()
              .map(PersonEntity::getId)
              .map(this::personLocation)
              .map(HALLink.Builder::new)
              .map(HALLink.Builder::build)
              .collect(Collectors.toList()));
    }
    if (personEntity.getParents() != null && !personEntity.getParents().isEmpty()) {
      personResource.setParentsLinks(
          personEntity.getParents().stream()
              .map(PersonEntity::getId)
              .map(this::personLocation)
              .map(HALLink.Builder::new)
              .map(HALLink.Builder::build)
              .collect(Collectors.toList()));
    }
    return personResource;
  }

  /**
   * Creates the location to the {@link PersonEntity} with the given {@code id}. The location is
   * based on the base URI of the request context and therefore will contain the same protocol, host
   * and port the client used in the current request.
   *
   * @param id the {@linkplain PersonEntity#getId() id} of the {@link PersonEntity} the link should
   *     be created for.
   * @return the location of the {@link PersonEntity} in the context of the current request.
   */
  private URI personLocation(String id) {
    return uriInfo.getBaseUriBuilder().path(PersonEndpoint.class).path(id).build();
  }
}
