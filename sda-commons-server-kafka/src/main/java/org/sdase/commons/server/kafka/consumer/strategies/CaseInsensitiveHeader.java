package org.sdase.commons.server.kafka.consumer.strategies;

import java.util.Locale;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.common.header.Header;

public class CaseInsensitiveHeader implements Header {
  private final Header originalHeader;

  public CaseInsensitiveHeader(Header originalHeader) {
    if (originalHeader == null) {
      throw new IllegalArgumentException("Header can not be null.");
    }
    this.originalHeader = originalHeader;
  }

  @Override
  public String key() {
    if (StringUtils.isBlank(originalHeader.key())) {
      return null;
    }
    return originalHeader.key().toLowerCase(Locale.ROOT);
  }

  @Override
  public byte[] value() {
    return originalHeader.value();
  }
}
