package org.sdase.commons.server.hibernate.example.rest;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import io.dropwizard.hibernate.UnitOfWork;
import java.net.URI;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.sdase.commons.server.dropwizard.ContextAwareEndpoint;
import org.sdase.commons.server.hibernate.example.db.manager.PersonManager;
import org.sdase.commons.server.hibernate.example.db.model.PersonEntity;
import org.sdase.commons.server.hibernate.example.rest.model.PersonResource;

/** Sample service endpoint to create and read person entities from hibernate */
@Path("/persons")
public class PersonsEndPoint implements ContextAwareEndpoint {

  /** DAO for accessing persons in the database */
  private final PersonManager personManager;

  @Context UriInfo uriInfo;

  public PersonsEndPoint(PersonManager personManager) {
    this.personManager = personManager;
  }

  /**
   * creates a person entity in the hibernate database and
   *
   * @param p person to store
   * @return response object with the url of the newly created person in the location header
   */
  @POST // maps to '/persons'
  @Consumes(APPLICATION_JSON)
  @UnitOfWork // marks this method as a transactional resource method with
  // database access
  public Response createPerson(PersonResource p) {
    PersonEntity person = personManager.persist(toEntity(p));
    URI location =
        uriInfo
            .getBaseUriBuilder()
            .path(PersonsEndPoint.class)
            .path(PersonsEndPoint.class, "getPerson")
            .resolveTemplate("personId", person.getId())
            .build();
    return Response.created(location).build();
  }

  @GET
  @Path("/{personId}") // maps to '/persons/{personId}'
  @Produces(APPLICATION_JSON)
  @UnitOfWork // marks this method as a transactional resource method with
  // database access
  public PersonResource getPerson(@PathParam("personId") long personId) {
    return toResource(personManager.getById(personId));
  }

  /**
   * Creates a {@link PersonResource} representing the given {@link PersonEntity}. We do not want to
   * expose the underlying entities to our consumers. This way our API is independent from the
   * internal model which may be changed with an appropriate migration at any time.
   *
   * @param resource resource that should be mapped
   * @return entity corresponding to the resource
   */
  private PersonEntity toEntity(PersonResource resource) {
    PersonEntity result = new PersonEntity();
    result.setName(resource.getName());
    result.setId(resource.getId());
    return result;
  }

  /**
   * Creates a {@link PersonEntity} respresenting the given {@link PersonResource}.
   *
   * @param entity entity that should be mapped to resource
   * @return resource corresponding to the entity
   */
  private PersonResource toResource(PersonEntity entity) {
    PersonResource result = new PersonResource();
    result.setName(entity.getName());
    result.setId(entity.getId());
    return result;
  }
}
