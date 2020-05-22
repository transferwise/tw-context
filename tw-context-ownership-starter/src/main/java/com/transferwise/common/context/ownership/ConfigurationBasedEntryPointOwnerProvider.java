package com.transferwise.common.context.ownership;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Autowired;

public class ConfigurationBasedEntryPointOwnerProvider implements EntryPointOwnerProvider {

  @Autowired
  @Setter
  private TwContextOwnershipProperties properties;

  private volatile Map<String, Map<String, String>> groupNameOwnerMap;

  private final Pattern componentsSeparatingRegex = Pattern.compile("(?:\\\\.|[^:\\\\]++)*");

  @PostConstruct
  public void init() {
    groupNameOwnerMap = properties.getEntryPointToOwnerMappings().stream().map(this::getParts)
        .collect(Collectors.groupingBy(this::getGroupPart, Collectors.toMap(this::getNamePart, this::getOwnerPart)));
  }

  @Override
  public String getOwner(String entryPointGroup, String entryPointName) {
    Map<String, String> groupMap = groupNameOwnerMap.get(entryPointGroup);
    return groupMap == null ? null : groupMap.get(entryPointName);
  }

  protected String getGroupPart(String[] parts) {
    return parts[0];
  }

  protected String getNamePart(String[] parts) {
    return parts[1];
  }

  protected String getOwnerPart(String[] parts) {
    String owner = parts[2];
    if (properties.isValidateOwners()) {
      owner = StringUtils.lowerCase(owner);
      if (TwTeam.getByGithubTeam(owner) == null) {
        throw new IllegalArgumentException("TwTeam does not contain Github team of '" + owner + "'.");
      }
    }
    return owner;
  }

  protected String[] getParts(String config) {
    Matcher regexMatcher = componentsSeparatingRegex.matcher(config);
    String[] parts = new String[3];
    int i = 0;
    while (regexMatcher.find()) {
      if (!regexMatcher.group().isEmpty()) {
        if (i > 2) {
          throw new IllegalArgumentException("Invalid config `" + config + "` provided.");
        }
        String part = StringEscapeUtils.unescapeJava(regexMatcher.group());
        parts[i++] = part;
      }
    }
    if (i < 3) {
      throw new IllegalArgumentException("Invalid config `" + config + "` provided.");
    }
    return parts;
  }

}
