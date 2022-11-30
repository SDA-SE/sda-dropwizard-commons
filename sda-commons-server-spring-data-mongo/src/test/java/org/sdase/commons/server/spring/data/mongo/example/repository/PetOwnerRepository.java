package org.sdase.commons.server.spring.data.mongo.example.repository;

import org.bson.types.ObjectId;
import org.sdase.commons.server.spring.data.mongo.example.model.PetOwner;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface PetOwnerRepository extends PagingAndSortingRepository<PetOwner, ObjectId> {

  // additional custom finder methods go here
}
