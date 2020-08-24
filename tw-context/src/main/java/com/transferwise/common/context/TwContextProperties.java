package com.transferwise.common.context;

import java.time.Duration;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class TwContextProperties {

  /**
   * Used to multiply all timeouts with specified values.
   * 
   * <p>Useful for development environments, where timeouts' values set for production do not work well.
   */
  private Double timeoutMultiplier;
  /**
   * Used to add a set value to all timeouts.
   *
   * <p>Useful for development environments, where timeouts' values set for production do not work well.
   */
  private Duration timeoutAdditive;
}
