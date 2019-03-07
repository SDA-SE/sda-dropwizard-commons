package org.sdase.commons.shared.yaml;

import java.time.ZonedDateTime;

public class TimeAwareBean {

   private ZonedDateTime time;

   public ZonedDateTime getTime() {
      return time;
   }

   public TimeAwareBean setTime(ZonedDateTime time) {
      this.time = time;
      return this;
   }
}
