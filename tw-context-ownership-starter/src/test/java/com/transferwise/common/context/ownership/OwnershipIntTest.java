package com.transferwise.common.context.ownership;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.transferwise.common.context.TwContext;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = TestApplication.class)
public class OwnershipIntTest {

  @Autowired
  private EntryPointOwnerAttributesChangeListener entryPointOwnerAttributesChangeListener;

  @Test
  public void ownerShipIsMappedByConfiguration() {
    TwContext twContext = TwContext.current().createSubContext().asEntryPoint("Jobs", "testJob1");
    assertThat(twContext.getOwner()).isEqualTo("webapp-reliability");

    twContext.setName("Web", "/v1/profile/1 (GET)");
    assertThat(twContext.getOwner()).isEqualTo("profile-service");

    twContext.setName("Unknown", "Unknown");
    assertThat(twContext.getOwner()).isEqualTo("SRE");
  }

  @Test
  void entrypointsWithoutOwnerShouldBeLoggedOnce() {
    entryPointOwnerAttributesChangeListener.clearDefaultOwners();

    TwContext twContext = TwContext.current().createSubContext().asEntryPoint("Jobs", "testJob1");

    Logger logger = (Logger) LoggerFactory.getLogger(EntryPointOwnerAttributesChangeListener.class);
    ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
    listAppender.start();
    logger.addAppender(listAppender);
    try {
      twContext.setName("Unknown", "Unknown");
      assertThat(twContext.getOwner()).isEqualTo("SRE");
      assertThat(listAppender.list).hasSize(1);
      assertThat(listAppender.list).extracting(ILoggingEvent::getMessage, ILoggingEvent::getLevel)
          .containsExactly(Tuple.tuple("Entrypoint 'Unknown:Unknown' does not have an owner.", Level.WARN));

      twContext.setName("Unknown", "Unknown");
      assertThat(twContext.getOwner()).isEqualTo("SRE");
      assertThat(listAppender.list).as("Should not be logged as a warning anymore.").hasSize(1);

      twContext = TwContext.current().createSubContext().asEntryPoint("Generic", "Generic");
      assertThat(listAppender.list).as("Generic/Generic will not be warned about.").hasSize(1);
    } finally {
      // Unfortunately there is no method to remove the appender from the logger all-together.
      listAppender.stop();
    }
  }
}
