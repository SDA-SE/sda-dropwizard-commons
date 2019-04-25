package org.sdase.commons.server.morphia;

import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoCommandException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.morphia.Datastore;

import java.util.List;

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
      }
      else {
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
      // indexName is the only thing we know about the failed index creation, we must look for it in all collections
      for (String collectionName : this.datastore.getDB().getCollectionNames()) {
         DBCollection collection = this.datastore.getDB().getCollection(collectionName);
         List<DBObject> indices = collection.getIndexInfo();
         for (DBObject dbIndex : indices) {
            if (dbIndex.get("name").equals(indexName)) {
               LOG.info("Dropping index {} in collection {} to create a new index.", indexName, collectionName);
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
