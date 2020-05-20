package com.transferwise.common.context.ownership;

public interface EntryPointOwnerProvider {
  String getOwner(String entryPointGroup, String entryPointName);
}
