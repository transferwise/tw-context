package com.transferwise.common.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = TestApplication.class)
public class ApplicationTest {

  @Autowired
  private UnitOfWorkFactory unitOfWorkFactory;

  @Test
  void applicationIsConfigured() {
    assertEquals("123", unitOfWorkFactory.asEntryPoint("A", "B").execute(() -> "123"));

    assertTrue(TwContext.getExecutionInterceptors().stream()
        .anyMatch(i -> i instanceof TwContextUniqueEntryPointsLimitingInterceptor));

    AtomicInteger genericCount = new AtomicInteger();
    for (int i = 0; i < 2000; i++) {
      unitOfWorkFactory.asEntryPoint(String.valueOf(i), String.valueOf(i)).execute(() -> {
        String group = TwContext.current().getGroup();
        String name = TwContext.current().getName();

        if ("Generic".equals(group) && "Generic".equals(name)) {
          genericCount.incrementAndGet();
        }
      });
    }

    assertEquals(1001, genericCount.get());
  }
}
