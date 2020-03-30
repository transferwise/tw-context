package com.transferwise.common.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

public class TwContextTest {

  @Test
  void testContextHierarchy() {
    TwContext context = TwContext.current().createSubContext();

    assertFalse(context.isRoot());
    assertTrue(context.getParent().isRoot());

    String testKey = "TestKey";
    context.put(testKey, "0");

    assertEquals("0", context.get(testKey));

    context.execute(() -> {
      assertEquals("0", context.get(testKey));
      assertEquals(context, TwContext.current());

      TwContext subContext = context.createSubContext();
      assertEquals("0", subContext.get(testKey));
      subContext.put(testKey, "1");
      assertEquals("1", subContext.get(testKey));
      assertEquals("1", subContext.getNew(testKey));
      assertEquals("0", context.get(testKey));

      subContext.execute(() -> assertEquals("1", TwContext.current().get(testKey)));
      assertEquals("0", TwContext.current().get(testKey));
    });

    assertTrue(TwContext.current().isRoot());
  }

  @Test
  void testMdcIntegrationUsingStaticPutMethod() {
    TwContext context = TwContext.current().createSubContext();
    String testKey = "testKey";
    String testValue = "testValue";
    String testValue1 = "testValue1";

    context.execute(() -> {
      TwContext.putCurrentMdc(testKey, testValue);
      assertThat(MDC.get(testKey)).isEqualTo(testValue);

      TwContext nestedContext = context.createSubContext();
      nestedContext.execute(() -> {
        TwContext.putCurrentMdc(testKey, testValue1);
        assertThat(MDC.get(testKey)).isEqualTo(testValue1);

        TwContext nestedContext1 = nestedContext.createSubContext();
        nestedContext1.execute(() -> {
          TwContext.putCurrentMdc(testKey, null);
          assertThat(MDC.get(testKey)).isNull();
        });
        assertThat(MDC.get(testKey)).isEqualTo(testValue1);
      });

      assertThat(MDC.get(testKey)).isEqualTo(testValue);
    });

    assertThat(MDC.get(testKey)).isNull();
  }

  @Test
  void testMdcIntegration() {
    TwContext context = TwContext.current().createSubContext();
    String testKey = "testKey";
    String testValue = "testValue";
    String testValue1 = "testValue1";

    context.putMdc(testKey, testValue);
    context.execute(() -> {
      assertThat(MDC.get(testKey)).isEqualTo(testValue);

      TwContext nestedContext = context.createSubContext();
      nestedContext.putMdc(testKey, testValue1);
      nestedContext.execute(() -> {
        assertThat(MDC.get(testKey)).isEqualTo(testValue1);

        TwContext nestedContext1 = nestedContext.createSubContext();
        nestedContext1.putMdc(testKey, null);
        nestedContext1.execute(() -> {
          assertThat(MDC.get(testKey)).isNull();
        });
        assertThat(MDC.get(testKey)).isEqualTo(testValue1);
      });

      assertThat(MDC.get(testKey)).isEqualTo(testValue);
    });

    assertThat(MDC.get(testKey)).isNull();
  }

}
