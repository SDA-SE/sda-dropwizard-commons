package com.sdase.commons.server.hibernate.test;

import com.sdase.commons.server.hibernate.test.model.Person;
import io.dropwizard.hibernate.AbstractDAO;
import io.dropwizard.hibernate.UnitOfWork;
import org.hibernate.SessionFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/persons")
public class PersonEndPoint extends AbstractDAO<Person> {

   @Context
   private UriInfo uriInfo;

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
      URI location = uriInfo.getBaseUriBuilder()
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
