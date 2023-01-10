package org.sdase.commons.server.morphia;

import com.mongodb.MongoCommandException;
import com.mongodb.client.ListIndexesIterable;
import com.mongodb.client.MongoCollection;
import dev.morphia.Datastore;
import dev.morphia.mapping.MappedClass;
import java.util.List;
import java.util.stream.Collectors;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * On top of Morphia's {@linkplain Datastore#ensureIndexes() implementation to automatically create
 * indexes}, the {@code IndexEnsurer} can force to create the indexes for a collection. It reacts on
 * failures due to already existing indexes that are changed by deleting them and retrying. It does
 * not support renaming of indexes.
 *
 * <p>Consumers <strong>should not use</strong> the {@code IndexEnsurer} directly to force ensuring
 * indexes, but set the {@linkplain MorphiaBundle.EnsureIndexesConfigBuilder#forceEnsureIndexes()
 * configuration option} at the {@link MorphiaBundle}.
 *
 * <p>In rare cases like renaming an index, the {@code IndexEnsurer} may be used in a custom
 * migration implementation to create the renamed index after a custom query removed the old index.
 * In such cases, the {@code IndexEnsurer} must be {@linkplain
 * MorphiaBundle.EnsureIndexesConfigBuilder#skipEnsureIndexes() disabled} when configuring the
 * {@link MorphiaBundle}.
 */
public class IndexEnsurer {

  private static final Logger LOG = LoggerFactory.getLogger(IndexEnsurer.class);

  private final Datastore datastore;
  private final boolean forceEnsureIndex;

  public IndexEnsurer(Datastore datastore, boolean forceEnsureIndex) {
    this.datastore = datastore;
    this.forceEnsureIndex = forceEnsureIndex;
  }

  public void ensureIndexes() {
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
    // Only consider collections of mapped entities and not system collections like `system.profile`
    List<String> collections =
        datastore.getMapper().getMappedClasses().stream()
            .filter(it -> it.getEntityAnnotation() != null)
            .map(MappedClass::getCollectionName) // NOSONAR
            .collect(Collectors.toList());
    for (String collectionName : collections) {
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
