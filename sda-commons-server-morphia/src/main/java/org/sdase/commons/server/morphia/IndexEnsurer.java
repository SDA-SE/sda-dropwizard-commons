package org.sdase.commons.server.morphia;

import com.mongodb.MongoCommandException;
import com.mongodb.client.ListIndexesIterable;
import com.mongodb.client.MongoCollection;
import dev.morphia.Datastore;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class IndexEnsurer {

  private static final Logger LOG = LoggerFactory.getLogger(IndexEnsurer.class);

  private Datastore datastore;
  private final boolean forceEnsureIndex;

  IndexEnsurer(Datastore datastore, boolean forceEnsureIndex) {
    this.datastore = datastore;
    this.forceEnsureIndex = forceEnsureIndex;
  }

  void ensureIndexes() {
    ensureIndexes(this.forceEnsureIndex);
  }

  private void ensureIndexes(boolean force) {
    if (force) {
      forceEnsureIndexes();
    } else {
      this.datastore.ensureIndexes();
    }
  }

  private void forceEnsureIndexes() {
    try {
      this.datastore.ensureIndexes();
    } catch (MongoCommandException e) {
      dropIndexInAllCollections(e);
      forceEnsureIndexes();
    }
  }

  private void dropIndexInAllCollections(MongoCommandException e) {
    if (!e.getMessage().contains(" name: ")) {
      LOG.error("Failed to ensure indexes");
      throw e;
    }
    boolean indexDropped = false;
    String errorMessageStartingWithIndexName = e.getErrorMessage().split("name:")[1].trim();
    String indexName = errorMessageStartingWithIndexName.split("\\s")[0].trim();
    // indexName is the only thing we know about the failed index creation, we must look for it in
    // all collections
    for (String collectionName : datastore.getDatabase().listCollectionNames()) {
      MongoCollection<Document> collection =
          this.datastore.getDatabase().getCollection(collectionName);
      ListIndexesIterable<Document> indices = collection.listIndexes();
      for (Document dbIndex : indices) {
        if (dbIndex.get("name").equals(indexName)) {
          LOG.info(
              "Dropping index {} in collection {} to create a new index.",
              indexName,
              collectionName);
          collection.dropIndex(indexName);
          indexDropped = true;
        }
      }
    }
    if (!indexDropped) {
      throw new IllegalStateException("Failed to recreate index " + indexName, e);
    }
  }
}
