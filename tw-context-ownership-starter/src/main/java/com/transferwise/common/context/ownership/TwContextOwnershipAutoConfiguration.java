package com.transferwise.common.context.ownership;

import com.transferwise.common.context.TwContext;
import com.transferwise.common.context.TwContextAttributeChangeListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(value = "tw-context.ownership.enabled", havingValue = "true", matchIfMissing = true)
public class TwContextOwnershipAutoConfiguration {

  @Bean
  public EntryPointOwnerProviderRegistry entryPointOwnerProviderRegistry() {
    return new DefaultEntryPointOwnerProviderRegistry();
  }

  @Bean
  public TwContextAttributeChangeListener twContextOwnershipAttributesChangeListener() {
    EntryPointOwnerAttributesChangeListener listener = new EntryPointOwnerAttributesChangeListener();
    TwContext.addAttributeChangeListener(listener);
    return listener;
  }

  @Bean
  @ConfigurationProperties(prefix = "tw-context.ownership", ignoreInvalidFields = true)
  @ConditionalOnMissingBean(TwContextOwnershipProperties.class)
  public TwContextOwnershipProperties twContextOwnershipProperties() {
    return new TwContextOwnershipProperties();
  }

  @Bean
  public ConfigurationBasedEntryPointOwnerProvider twContextOwnershipConfigurationBasedEntryPointOwnerProvider() {
    return new ConfigurationBasedEntryPointOwnerProvider();
  }
}
