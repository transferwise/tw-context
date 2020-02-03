package com.transferwise.common.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

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
}
