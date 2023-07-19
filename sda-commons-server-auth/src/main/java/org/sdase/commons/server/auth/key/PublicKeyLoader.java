package org.sdase.commons.server.auth.key;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Loads public keys from various locations, converts them to Java Keys and caches them. */
public class PublicKeyLoader {

  private static final Logger LOGGER = LoggerFactory.getLogger(PublicKeyLoader.class);
  private final Map<String, LoadedPublicKey> keysByKid = new ConcurrentHashMap<>();
  private final Map<String, LoadedPublicKey> keysByx5t = new ConcurrentHashMap<>();
  private final Set<LoadedPublicKey> keysWithoutAnyId = new CopyOnWriteArraySet<>();
  private final Map<KeySource, Boolean> keySources = new ConcurrentHashMap<>();

  private final Object loadingSemaphore = new Object();

  /**
   * @return All keys that have been registered without kid in the order they have been added.
   */
  public List<LoadedPublicKey> getKeysWithoutAnyId() {
    if (keysWithoutAnyId.isEmpty()) {
      // we may need to avoid reloading every time and delay reloads if reloaded keys just moments
      // ago
      reloadKeys();
    }
    return new ArrayList<>(keysWithoutAnyId);
  }

  /**
   * @deprecated Deprecated due to the extension of the loader managing more than one id
   */
  @Deprecated
  public List<LoadedPublicKey> getKeysWithoutId() {
    return getKeysWithoutAnyId();
  }

  public LoadedPublicKey getLoadedPublicKey(String kid, String x5t) {
    if (StringUtils.isBlank(kid) && StringUtils.isBlank(x5t)) {
      return null;
    }
    LoadedPublicKey existingKey = getKeyFromLocalStore(kid, x5t);
    if (existingKey != null) {
      return existingKey;
    }
    // we may need to avoid reloading every time and delay reloads if reloaded keys just moments ago
    reloadKeys();
    return getKeyFromLocalStore(kid, x5t);
  }

  public LoadedPublicKey getLoadedPublicKey(String kid) {
    return getLoadedPublicKey(kid, null);
  }

  private LoadedPublicKey getKeyFromLocalStore(String kid, String x5t) {
    if (StringUtils.isBlank(kid) && StringUtils.isBlank(x5t)) {
      return null;
    }
    LoadedPublicKey key = null;
    if (StringUtils.isNotBlank(kid)) {
      key = keysByKid.get(kid);
    }
    if (key == null && StringUtils.isNotBlank(x5t)) {
      key = keysByx5t.get(x5t);
    }
    return key;
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
    return (int)
        Stream.concat(
                Stream.concat(keysByKid.values().stream(), keysByx5t.values().stream()),
                keysWithoutAnyId.stream())
            .distinct()
            .count();
  }

  void reloadKeys() {
    synchronized (loadingSemaphore) {
      keySources
          .keySet()
          .forEach(
              ks -> {
                keySources.put(ks, true);
                reloadFromKeySource(ks);
              });
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
    keysWithoutAnyId.removeIf(k -> keySource.equals(k.getKeySource()) && !newKeys.contains(k));
    updateKeysByKid(keySource, newKeys);
    updateKeysByX5t(keySource, newKeys);
  }

  private void updateKeysByKid(KeySource keySource, List<LoadedPublicKey> newKeys) {
    Set<String> newKeyIds =
        newKeys.stream() // NOSONAR squid:S1854 this assignment is not useless
            .map(LoadedPublicKey::getKid)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    keysByKid.values().stream()
        .filter(k -> keySource.equals(k.getKeySource()))
        .map(LoadedPublicKey::getKid)
        .filter(kid -> !newKeyIds.contains(kid))
        .forEach(keysByKid::remove);
  }

  private void updateKeysByX5t(KeySource keySource, List<LoadedPublicKey> newKeys) {
    @SuppressWarnings("DuplicatedCode")
    Set<String> newKeyIds =
        newKeys.stream() // NOSONAR squid:S1854 this assignment is not useless
            .map(LoadedPublicKey::getX5t)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    keysByx5t.values().stream()
        .filter(k -> keySource.equals(k.getKeySource()))
        .map(LoadedPublicKey::getX5t)
        .filter(x5t -> !newKeyIds.contains(x5t))
        .forEach(keysByx5t::remove);
  }

  private void loadAllNewKeys() {
    try {
      synchronized (loadingSemaphore) {
        keySources.keySet().stream()
            .filter(ks -> !keySources.get(ks))
            .map(
                ks -> {
                  keySources.put(ks, true);
                  return silentlyLoadKeysFromSource(ks);
                })
            .flatMap(List::stream)
            .forEach(this::addKey);
      }
    } catch (Throwable t) { // NOSONAR
      // Catch information about any error that occurs in this method.
      // This method is called in a dedicated Thread which error information might get lost.
      LOGGER.error("Failed to initially load keys in a dedicated Thread", t);
    }
  }

  private List<LoadedPublicKey> silentlyLoadKeysFromSource(KeySource keySource) {
    try {
      return keySource.loadKeysFromSource();
    } catch (KeyLoadFailedException e) {
      LOGGER.error("An error occurred while loading new keys from {}", keySource, e);
      return Collections.emptyList();
    }
  }

  private void addKey(LoadedPublicKey key) {
    if (StringUtils.isBlank(key.getKid()) && StringUtils.isBlank(key.getX5t())) {
      keysWithoutAnyId.add(key);
    }
    if (StringUtils.isNotBlank(key.getKid())) {
      keysByKid.put(key.getKid(), key);
    }
    if (StringUtils.isNotBlank(key.getX5t())) {
      keysByx5t.put(key.getX5t(), key);
    }
  }
}
