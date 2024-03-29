package org.sdase.commons.server.openapi.example.people.rest;

import io.openapitools.jackson.dataformat.hal.HALLink;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.sdase.commons.server.dropwizard.ContextAwareEndpoint;

/** Example endpoint for getting information about people. */
@PermitAll // Require authentication for this endpoint. Take care the annotation is applied to the
// class, not the service interface!
public class PersonEndpoint implements PersonService, ContextAwareEndpoint {

  private final Map<Integer, CreatePersonResource> people =
      Collections.synchronizedMap(new LinkedHashMap<>());

  /**
   * Information about the requested URI. Jersey will inject a Proxy of {@code UriInfo} that is
   * aware of the request scope.
   */
  @Context private UriInfo uriInfo;

  public PersonEndpoint() {
    people.put(
        0,
        new CreatePersonResource()
            .setFirstName("Max")
            .setLastName("Mustermann")
            .setAddresses(
                Collections.singletonList(
                    new AddressResource().setStreet("Reeperbahn 1").setCity("Hamburg"))));
    people.put(
        1,
        new CreatePersonResource()
            .setFirstName("John")
            .setLastName("Doe")
            .setAddresses(
                Collections.singletonList(
                    new AddressResource().setStreet("Unter den Linden 5").setCity("Berlin"))));
  }

  @Override
  public List<PersonResource> findAllPeople() {
    synchronized (people) {
      return people.entrySet().stream()
          .map(e -> toResourceWithSelfLink(e.getKey(), e.getValue()))
          .toList();
    }
  }

  @Override
  public Response createPerson(CreatePersonResource person) {
    int id = people.size();
    people.put(id, person);

    return Response.created(getPersonUri(id)).build();
  }

  @Override
  public PersonResource findPersonById(int personId) {
    if (!people.containsKey(personId)) {
      throw new NotFoundException();
    }

    return toResourceWithSelfLink(personId, people.get(personId));
  }

  private URI getPersonUri(int id) {
    return uriInfo
        .getBaseUriBuilder()
        .path(PersonService.class)
        .path(PersonService.class, "findPersonById")
        .resolveTemplate("personId", id)
        .build();
  }

  private PersonResource toResourceWithSelfLink(int id, CreatePersonResource source) {
    return new PersonResource()
        .setSelfLink(new HALLink.Builder(getPersonUri(id)).build())
        .setFirstName(source.getFirstName())
        .setLastName(source.getLastName())
        .setAddresses(
            source.getAddresses().stream()
                .map(a -> new AddressResource().setCity(a.getCity()).setStreet(a.getStreet()))
                .collect(Collectors.toList()));
  }
}
