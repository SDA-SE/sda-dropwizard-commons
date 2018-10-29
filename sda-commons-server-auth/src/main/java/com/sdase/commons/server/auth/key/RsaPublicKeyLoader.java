package com.sdase.commons.server.auth.key;

import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

/**
 * Loads public keys from various locations, converts them to Java Keys and caches them.
 */
public class RsaPublicKeyLoader {

   private Map<String, LoadedPublicKey> keysByKid = new ConcurrentHashMap<>();

   private Set<LoadedPublicKey> keysWithoutKeyId = new CopyOnWriteArraySet<>();

   private Map<KeySource, Boolean> keySources = new ConcurrentHashMap<>();

   private final Object loadingSemaphore = new Object();

   /**
    * @return All keys that have been registered without kid in the order they have been added.
    */
   public List<RSAPublicKey> getKeysWithoutId() {
      if (keysWithoutKeyId.isEmpty()) {
         // we may need to avoid reloading every time and delay reloads if reloaded keys just moments ago
         reloadKeys();
      }
      return keysWithoutKeyId.stream().map(LoadedPublicKey::getPublicKey).collect(Collectors.toList());
   }

   public RSAPublicKey getKey(String kid) {
      if (kid == null) {
         return null;
      }
      LoadedPublicKey key = keysByKid.get(kid);
      if (key != null) {
         return key.getPublicKey();
      }
      // we may need to avoid reloading every time and delay reloads if reloaded keys just moments ago
      reloadKeys();
      key = keysByKid.get(kid);
      if (key != null) {
         return key.getPublicKey();
      }
      return null;
   }

   public void addKeySource(KeySource keySource) {
      this.keySources.put(keySource, false);
      // avoid to slow down startup of the application
      new Thread(this::loadAllNewKeys).start();
   }

   public int getTotalNumberOfKeySources() {
      return keySources.size();
   }

   public int getTotalNumberOfKeys() {
      return keysWithoutKeyId.size() + keysByKid.size();
   }

   private void reloadKeys() {
      synchronized (loadingSemaphore) {
         keySources.keySet().stream()
               .peek(ks -> keySources.put(ks, true))
               .forEach(this::reloadFromKeySource);
      }
   }

   private void reloadFromKeySource(KeySource keySource) {
      Optional<List<LoadedPublicKey>> loadedPublicKeys = keySource.reloadKeysFromSource();
      if (loadedPublicKeys.isPresent()) {
         List<LoadedPublicKey> keys = loadedPublicKeys.get();
         removeOldKeysFromSource(keySource, keys);
         keys.forEach(this::addKey);
      }
   }

   private void removeOldKeysFromSource(KeySource keySource, List<LoadedPublicKey> newKeys) {
      keysWithoutKeyId.removeIf(k -> keySource.equals(k.getKeySource()) && !newKeys.contains(k));
      Set<String> newKeyIds = newKeys.stream()
            .map(LoadedPublicKey::getKid)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
      keysByKid.values().stream()
            .filter(k -> keySource.equals(k.getKeySource()))
            .map(LoadedPublicKey::getKid)
            .filter(kid -> !newKeyIds.contains(kid))
            .forEach(keysByKid::remove);
   }

   private void loadAllNewKeys() {
      synchronized (loadingSemaphore) {
         keySources.keySet().stream()
               .filter(ks -> !keySources.get(ks))
               .peek(ks -> keySources.put(ks, true))
               .map(KeySource::loadKeysFromSource)
               .flatMap(List::stream)
               .forEach(this::addKey);
      }
   }

   private void addKey(LoadedPublicKey key) {
      if (key.getKid() == null) {
         keysWithoutKeyId.add(key);
      }
      else {
         keysByKid.put(key.getKid(), key);
      }
   }
}
