package org.sdase.commons.server.swagger;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.openapitools.jackson.dataformat.hal.HALLink;
import io.openapitools.jackson.dataformat.hal.annotation.EmbeddedResource;
import io.openapitools.jackson.dataformat.hal.annotation.Link;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Resource
@ApiModel("HouseResource")
public class HouseResource {
   @Link
   @ApiModelProperty("Link relation 'self': The HAL link referencing this file.")
   private HALLink self;

   @Link("partners")
   @ApiModelProperty(value = "list of partners that live in the house (Link)")
   private List<HALLink> partnersLink = new ArrayList<>();

   @EmbeddedResource
   @ApiModelProperty(value = "list of partners that live in the house (Embedded)")
   private final List<PartnerResource> partners = new ArrayList<>();

   @Link("animals")
   @ApiModelProperty(value = "list of animals that live in the house (Link)")
   private List<HALLink> animalsLink = new ArrayList<>();

   @EmbeddedResource
   @ApiModelProperty(value = "list of animals that live in the house (Embedded)")
   private final List<AnimalResource> animals = new ArrayList<>();

   @JsonCreator
   public HouseResource(@JsonProperty("partners") List<PartnerResource> partners,
         @JsonProperty("animals") List<AnimalResource> animals, HALLink self) {

      this.partners.addAll(partners);
      this.animals.addAll(animals);
      this.self = self;
   }

   public HALLink getSelf() {
      return self;
   }

   public List<HALLink> getPartnersLink() {
      return partnersLink;
   }

   public List<PartnerResource> getPartners() {
      return partners;
   }

   public List<HALLink> getAnimalsLink() {
      return animalsLink;
   }

   public List<AnimalResource> getAnimals() {
      return animals;
   }
}
