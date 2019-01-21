package org.sdase.commons.server.swagger.example.people.rest;

import io.openapitools.jackson.dataformat.hal.HALLink;

import javax.annotation.security.PermitAll;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Example endpoint for getting information about people.
 */
@PermitAll // Require authentication for this endpoint. Take care the annotation is applied to the
           // class, not the service interface!
public class PersonEndpoint implements PersonService {

   private Map<Integer, PersonResource> people = new LinkedHashMap<>();

   /**
    * Information about the requested URI. Jersey will inject a Proxy of
    * {@code UriInfo} that is aware of the request scope.
    */
   @Context
   private UriInfo uriInfo;

   public PersonEndpoint() {
      people.put(0, new PersonResource()
            .setFirstName("Max")
            .setLastName("Mustermann")
            .setAddresses(
                  Collections.singletonList(new AddressResource()
                        .setStreet("Reeperbahn 1")
                        .setCity("Hamburg"))));
      people.put(1, new PersonResource()
            .setFirstName("John")
            .setLastName("Doe")
            .setAddresses(Collections
                  .singletonList(new AddressResource()
                        .setStreet("Unter den Linden 5")
                        .setCity("Berlin"))));
   }

   public List<PersonResource> findAllPeople() {
      return people
          .entrySet()
          .stream()
          .map(e -> toResourceWithSelfLink(e.getKey(), e.getValue()))
          .collect(Collectors.toList());
   }

   public Response createPeople(PersonResource person) {
      int id = people.size();
      people.put(id, person);

      return Response.created(getPersonUri(id)).build();
   }

   public PersonResource findPersonById(int personId) {
      if (!people.containsKey(personId)) {
         throw new NotFoundException();
      }

      return toResourceWithSelfLink(personId, people.get(personId));
   }

   private PersonResource toResourceWithSelfLink(int id, PersonResource source) {
      return new PersonResource()
            .setSelfLink(new HALLink.Builder(getPersonUri(id)).build())
            .setFirstName(source.getFirstName())
            .setLastName(source.getLastName())
            .setAddresses(source.getAddresses().stream().map(
                  a -> new AddressResource()
                        .setCity(a.getCity())
                        .setStreet(a.getStreet()))
                  .collect(Collectors.toList()));
   }

   private URI getPersonUri(int id) {
      return uriInfo
          .getBaseUriBuilder()
          .path(PersonResource.class)
          .resolveTemplate("personId", id)
          .build();
   }
}
