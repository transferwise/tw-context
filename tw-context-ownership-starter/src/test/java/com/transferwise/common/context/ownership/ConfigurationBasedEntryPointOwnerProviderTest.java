package com.transferwise.common.context.ownership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

public class ConfigurationBasedEntryPointOwnerProviderTest {

  @Test
  void parsingOfDifferentOwnersMappings() {
    TwContextOwnershipProperties props = new TwContextOwnershipProperties();
    props.setValidateOwners(false);
    props.getEntryPointToOwnerMappings().add("Web:/v1:Kristo");
    props.getEntryPointToOwnerMappings().add("Web:/v1/quote/{quoteId} \\:GET:Yurii");
    props.getEntryPointToOwnerMappings().add("Web:/v1/profile/{profileId}:Lauri\\\\");

    ConfigurationBasedEntryPointOwnerProvider provider = createProvider(props);

    assertThat(provider.getOwner("Web", "/v1")).isEqualTo("Kristo");
    assertThat(provider.getOwner("Web", "/v1/quote/{quoteId} :GET")).isEqualTo("Yurii");
    assertThat(provider.getOwner("Web", "/v1/profile/{profileId}")).isEqualTo("Lauri\\");
    assertThat(provider.getOwner("a", "b")).isNull();
  }

  @Test
  void wrongConfigIsNotAccepted() {
    assertThatThrownBy(() -> {
      getProvider("Web:/v1", false).getOwner("a", "b");
    }).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void ownerIsValidatedAgainstGithubTeam() {
    assertThat(getProvider("Web:endpoint:webapp-reliability", true).getOwner("Web", "endpoint")).isEqualTo("webapp-reliability");

    assertThatThrownBy(() -> {
      getProvider("Web:endpoint:webapp-hackers", true).getOwner("Web", "endpoint");
    }).isInstanceOf(IllegalArgumentException.class);
  }

  protected ConfigurationBasedEntryPointOwnerProvider getProvider(String configLine, boolean validate) {
    TwContextOwnershipProperties props = new TwContextOwnershipProperties();
    props.setValidateOwners(validate);
    props.getEntryPointToOwnerMappings().add(configLine);

    ConfigurationBasedEntryPointOwnerProvider provider = createProvider(props);

    return provider;
  }

  protected ConfigurationBasedEntryPointOwnerProvider createProvider(TwContextOwnershipProperties props) {
    ConfigurationBasedEntryPointOwnerProvider provider = new ConfigurationBasedEntryPointOwnerProvider();
    provider.setProperties(props);
    provider.init();
    return provider;
  }
}
