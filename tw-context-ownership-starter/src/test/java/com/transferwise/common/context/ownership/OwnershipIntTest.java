package com.transferwise.common.context.ownership;

import static org.assertj.core.api.Assertions.assertThat;

import com.transferwise.common.context.TwContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = TestApplication.class)
public class OwnershipIntTest {

  @Test
  public void ownerShipIsMappedByConfiguration() {
    TwContext twContext = TwContext.current().createSubContext().asEntryPoint("Jobs", "testJob1");
    assertThat(twContext.getOwner()).isEqualTo("webapp-reliability");

    twContext.setName("Web", "/v1/profile/1 (GET)");
    assertThat(twContext.getOwner()).isEqualTo("profile-service");

    twContext.setName("Unknown", "Unknown");
    assertThat(twContext.getOwner()).isEqualTo("SRE");
  }
}
