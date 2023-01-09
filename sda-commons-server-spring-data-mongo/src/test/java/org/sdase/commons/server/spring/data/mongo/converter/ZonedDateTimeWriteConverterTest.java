/*
 * Copyright 2022- SDA SE Open Industry Solutions (https://www.sda.se)
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package org.sdase.commons.server.spring.data.mongo.converter;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZonedDateTime;
import java.util.Date;
import org.junit.jupiter.api.Test;

class ZonedDateTimeWriteConverterTest {

  ZonedDateTimeWriteConverter converter = ZonedDateTimeWriteConverter.INSTANCE;

  @Test
  void shouldEncodeZonedDateTime() {
    // when
    Date result = converter.convert(ZonedDateTime.parse("2019-02-21T17:22:53+01:00[Europe/Paris]"));
    // then UTC mapped
    assertThat(result).isEqualTo("2019-02-21T16:22:53.000Z");
  }
}
