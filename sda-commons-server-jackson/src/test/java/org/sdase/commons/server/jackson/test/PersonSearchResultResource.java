package org.sdase.commons.server.jackson.test;

import java.util.List;
import org.sdase.commons.server.jackson.EnableFieldFilter;

@EnableFieldFilter
public class PersonSearchResultResource {

  private List<PersonSearchItemResource> results;

  public List<PersonSearchItemResource> getResults() {
    return results;
  }

  public PersonSearchResultResource setResults(List<PersonSearchItemResource> results) {
    this.results = results;
    return this;
  }
}
