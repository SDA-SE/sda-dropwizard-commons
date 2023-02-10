package org.sdase.commons.server.spring.data.mongo.metadata.test.model;

import org.sdase.commons.server.dropwizard.metadata.DetachedMetadataContext;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
public class BusinessEntity {

  @Id private String id;

  private DetachedMetadataContext metadata;

  public String getId() {
    return id;
  }

  public BusinessEntity setId(String id) {
    this.id = id;
    return this;
  }

  public DetachedMetadataContext getMetadata() {
    return metadata;
  }

  public BusinessEntity setMetadata(DetachedMetadataContext metadata) {
    this.metadata = metadata;
    return this;
  }
}
