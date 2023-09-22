package com.transferwise.common.context;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.commons.lang3.mutable.MutableObject;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

public class TwContextAttributePutListenerTest {

  @Test
  void nameAttributeIsChangedAfterGroupAttribute() {
    MutableObject<Boolean> groupChanged = new MutableObject<>(false);
    MutableObject<Boolean> nameChanged = new MutableObject<>(false);
    TwContextAttributePutListener listener = (context, key, oldValue, newValue) -> {
      if (TwContext.GROUP_KEY.equals(key)) {
        groupChanged.setValue(true);
      } else if (TwContext.NAME_KEY.equals(key)) {
        if (!groupChanged.getValue()) {
          throw new IllegalStateException("Group was not changed before the name.");
        }
        nameChanged.setValue(true);
      }
    };
    TwContext.addAttributePutListener(listener);
    try {
      TwContext.current().createSubContext().asEntryPoint("SRE", "Task123");

      assertThat(nameChanged.getValue()).isTrue();
      assertThat(groupChanged.getValue()).isTrue();
    } finally {
      TwContext.removeAttributePutListener(listener);
    }
  }

  @Test
  void ownerCanBeSetWhenNameIsChanged() {
    TwContextAttributePutListener listener = (context, key, oldValue, newValue) -> {
      // Here we are making an assumption (covered by test in TwContext), that the name key is always changed after group key.
      // We could remove that assumption by deciding owner both on group key change and on name key change, but that would incur double work.
      if (TwContext.NAME_KEY.equals(key)) {
        if ("/ep1".equals(newValue)) {
          context.setOwner("Kristo");
        } else if ("/ep2".equals(newValue)) {
          context.setOwner("Yurii");
        }
      }
    };

    TwContext.addAttributePutListener(listener);
    try {
      TwContext twContext = TwContext.current().createSubContext().asEntryPoint("Test", "/ep1");
      assertThat(twContext.getOwner()).isEqualTo("Kristo");

      twContext.setName("Test", "/ep2");
      assertThat(twContext.getOwner()).isEqualTo("Yurii");

      twContext.execute(() -> {
        assertThat(MDC.get(TwContext.MDC_KEY_EP_OWNER)).isEqualTo("Yurii");
      });
    } finally {
      TwContext.removeAttributePutListener(listener);
    }
  }
}
