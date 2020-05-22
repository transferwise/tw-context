package com.transferwise.common.context.ownership;

public interface EntryPointOwnerProviderRegistry {
  String getOwner(String entryPointGroup, String entryPointName);
}
