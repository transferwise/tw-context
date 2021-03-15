package com.transferwise.common.context;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

public class TwContextTest {

  @AfterEach
  void setup() {
    TwContext.removeExecutionInterceptors();
  }

  @Test
  void testContextHierarchy() {
    TwContext context = TwContext.current().createSubContext();

    assertThat(context.isRoot()).isFalse();
    assertThat(context.getParent().isRoot()).isTrue();

    String testKey = "TestKey";
    context.put(testKey, "0");

    assertThat((String) context.get(testKey)).isEqualTo("0");

    context.execute(() -> {
      assertThat((String) context.get(testKey)).isEqualTo("0");
      assertThat(TwContext.current()).isEqualTo(context);

      TwContext subContext = context.createSubContext();
      assertThat((String) subContext.get(testKey)).isEqualTo("0");
      subContext.put(testKey, "1");
      assertThat((String) subContext.get(testKey)).isEqualTo("1");
      assertThat((String) subContext.getNew(testKey)).isEqualTo("1");
      assertThat((String) context.get(testKey)).isEqualTo("0");

      subContext.execute(() -> assertThat((String) TwContext.current().get(testKey))).isEqualTo("1");
      assertThat((String) TwContext.current().get(testKey)).isEqualTo("0");
    });

    assertThat(TwContext.current().isRoot()).isTrue();
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

  @Test
  void testInterceptorChain() {
    List<Integer> results = new ArrayList<>();
    for (int i = 0; i < 3; i++) {
      int finalI = i;
      TwContext.addExecutionInterceptor(new TwContextExecutionInterceptor() {
        @Override
        public boolean applies(TwContext context) {
          return finalI == 0 || finalI == 2;
        }

        @Override
        public <T> T intercept(TwContext context, Supplier<T> supplier) {
          results.add(finalI);
          return supplier.get();
        }
      });
    }

    TwContext.current().execute(() -> {
      // do nothing
    });

    assertThat(results).containsExactly(0, 2);
  }

}
