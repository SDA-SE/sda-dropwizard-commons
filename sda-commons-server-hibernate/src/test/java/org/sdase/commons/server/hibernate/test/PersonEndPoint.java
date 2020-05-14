package org.sdase.commons.server.hibernate.test;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import io.dropwizard.hibernate.AbstractDAO;
import io.dropwizard.hibernate.UnitOfWork;
import java.net.URI;
import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.hibernate.SessionFactory;
import org.sdase.commons.server.dropwizard.ContextAwareEndpoint;
import org.sdase.commons.server.hibernate.test.model.Person;

@Path("/persons")
public class PersonEndPoint extends AbstractDAO<Person> implements ContextAwareEndpoint {

  @Context private UriInfo uriInfo;

  @Inject
  public PersonEndPoint(SessionFactory sessionFactory) {
    super(sessionFactory);
  }

  @GET
  @Produces(APPLICATION_JSON)
  @UnitOfWork
  public List<Person> getPersons() {
    return list(query("select p from Person p order by p.id"));
  }

  @POST
  @Consumes(APPLICATION_JSON)
  @UnitOfWork
  public Response createPerson(Person p) {
    Person person = persist(p);
    URI location =
        uriInfo
            .getBaseUriBuilder()
            .path(PersonEndPoint.class)
            .path(PersonEndPoint.class, "getPerson")
            .resolveTemplate("personId", person.getId())
            .build();
    return Response.created(location).build();
  }

  @GET
  @Path("/{personId}")
  @Produces(APPLICATION_JSON)
  @UnitOfWork
  public Person getPerson(@PathParam("personId") long personId) {
    return get(personId);
  }
}
