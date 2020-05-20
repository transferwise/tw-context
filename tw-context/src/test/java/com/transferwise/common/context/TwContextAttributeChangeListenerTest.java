package com.transferwise.common.context;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

public class TwContextAttributeChangeListenerTest {

  @Test
  void ownerCanBeSetWhenNameIsChanged() {
    TwContextAttributeChangeListener listener = (context, key, oldValue, newValue) -> {
      if (TwContext.NAME_KEY.equals(key)) {
        if ("/ep1".equals(newValue)) {
          context.setOwner("Kristo");
        } else if ("/ep2".equals(newValue)) {
          context.setOwner("Yurii");
        }
      }
    };

    TwContext.addAttributeChangeListener(listener);

    TwContext twContext = TwContext.current().createSubContext().asEntryPoint("Test", "/ep1");
    assertThat(twContext.getOwner()).isEqualTo("Kristo");

    twContext.setName("Test", "/ep2");
    assertThat(twContext.getOwner()).isEqualTo("Yurii");

    twContext.execute(() -> {
      assertThat(MDC.get(TwContext.MDC_KEY_EP_OWNER)).isEqualTo("Yurii");
    });
  }
}
