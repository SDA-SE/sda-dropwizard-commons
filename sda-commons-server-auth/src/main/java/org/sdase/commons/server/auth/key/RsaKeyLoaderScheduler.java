package org.sdase.commons.server.auth.key;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RsaKeyLoaderScheduler {
  private static final Logger LOGGER = LoggerFactory.getLogger(RsaKeyLoaderScheduler.class);
  private static final int DEFAULT_INITIAL_DELAY = 5;
  private static final int DEFAULT_PERIOD = 5;
  private static final TimeUnit DEFAULT_TIMEUNIT = TimeUnit.MINUTES;

  private ScheduledExecutorService reloadKeysExecutorService;
  private RsaPublicKeyLoader keyLoader;

  private RsaKeyLoaderScheduler(
      RsaPublicKeyLoader keyLoader, ScheduledExecutorService reloadKeysExecutorService) {
    this.keyLoader = keyLoader;
    this.reloadKeysExecutorService = reloadKeysExecutorService;
  }

  public static RsaKeyLoaderScheduler create(
      RsaPublicKeyLoader keyLoader, ScheduledExecutorService reloadKeysExecutorService) {
    Validate.notNull(keyLoader, "keyLoader should not be null");
    Validate.notNull(reloadKeysExecutorService, "executorService should not be null");
    return new RsaKeyLoaderScheduler(keyLoader, reloadKeysExecutorService);
  }

  public RsaKeyLoaderScheduler start() {
    return internalStart(DEFAULT_INITIAL_DELAY, DEFAULT_PERIOD, DEFAULT_TIMEUNIT);
  }

  // please only call once!
  RsaKeyLoaderScheduler internalStart(int initialDelay, int period, TimeUnit timeUnit) {
    Runnable reloadKeysTask =
        () -> {
          try {
            keyLoader.reloadKeys();
          } catch (Exception e) {
            LOGGER.error("An error occurred while reloading public keys", e);
          }
        };

    // we set an initial delay because the keys are loaded at start-time of the service,
    // so we avoid to load them again immediately.
    reloadKeysExecutorService.scheduleAtFixedRate(reloadKeysTask, initialDelay, period, timeUnit);

    return this;
  }
}
