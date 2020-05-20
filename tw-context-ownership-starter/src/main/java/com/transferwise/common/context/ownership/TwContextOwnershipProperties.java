package com.transferwise.common.context.ownership;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class TwContextOwnershipProperties {

  private boolean enabled = true;
  
  /**
   * Default owner when no {@code com.transferwise.common.context.ownership.EntryPointOwnerProvider} decides an owner.
   */
  private String defaultOwner = "Generic";

  /**
   * Validates owners against Github teams names, defined in {@code com.transferwise.common.context.ownership.TwTeam}.
   */
  private boolean validateOwners = true;

  /**
   * Maps each entry point to one owner.
   * 
   * <p>The format for entries is {@code <entry point group>:<entry point name>:<owner>}.
   * ':' character in parts can be escaped with '\'.
   * 
   * <p>Usually the owner value is a Github team's name.
   * 
   * <p>Example: {@code Web:/v1/profiles/1 (GET):profile-service}
   */
  private List<String> entryPointToOwnerMappings = new ArrayList<>();
}
