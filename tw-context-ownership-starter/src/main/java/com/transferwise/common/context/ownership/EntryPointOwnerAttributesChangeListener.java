package com.transferwise.common.context.ownership;

import com.transferwise.common.context.TwContext;
import com.transferwise.common.context.TwContextAttributeChangeListener;
import org.springframework.beans.factory.annotation.Autowired;

public class EntryPointOwnerAttributesChangeListener implements TwContextAttributeChangeListener {

  @Autowired
  private EntryPointOwnerProviderRegistry entryPointOwnerProviderRegistry;
  @Autowired
  private TwContextOwnershipProperties properties;

  @Override
  public void attributeChanged(TwContext context, String key, Object oldValue, Object newValue) {
    if (TwContext.NAME_KEY.equals(key)) {
      String owner = entryPointOwnerProviderRegistry.getOwner(context.getGroup(), context.getName());
      if (owner == null) {
        if (properties.getDefaultOwner() != null) {
          context.setOwner(properties.getDefaultOwner());
        }
      } else {
        context.setOwner(owner);
      }
    }
  }
}
