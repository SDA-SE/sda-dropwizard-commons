package org.sdase.commons.server.spring.data.mongo.example.repository;

import org.bson.types.ObjectId;
import org.sdase.commons.server.spring.data.mongo.example.model.Person;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface PersonRepository extends PagingAndSortingRepository<Person, ObjectId> {

  // additional custom finder methods go here
}
