package com.transferwise.common.context;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import lombok.experimental.UtilityClass;

/**
 * Unfortunately Apache Common's DurationFormatUtils does not format milliseconds.
 *
 * <p>Package protected as not designed to be a generic class.
 */
@UtilityClass
class DurationFormatUtils {

  static String formatDuration(Duration duration) {
    StringBuffer sb = new StringBuffer();

    long milliseconds = duration.toMillis();
    long hours = TimeUnit.MILLISECONDS.toHours(milliseconds);
    milliseconds = milliseconds - TimeUnit.HOURS.toMillis(hours);

    long minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds);
    milliseconds = milliseconds - TimeUnit.MINUTES.toMillis(minutes);

    long seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds);
    milliseconds = milliseconds - TimeUnit.SECONDS.toMillis(seconds);

    if (hours != 0) {
      sb.append(hours).append(" hours ");
    }
    if (minutes != 0) {
      sb.append(minutes).append(" minutes ");
    }
    if (seconds != 0) {
      sb.append(seconds).append(" seconds ");
    }
    if (milliseconds != 0 || sb.length() == 0) {
      sb.append(milliseconds).append(" milliseconds");
    }
    if (sb.charAt(sb.length() - 1) == ' ') {
      return sb.substring(0, sb.length() - 1);
    } else {
      return sb.toString();
    }
  }
}
