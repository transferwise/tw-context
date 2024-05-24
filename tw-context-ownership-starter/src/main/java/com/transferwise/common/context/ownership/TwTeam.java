package com.transferwise.common.context.ownership;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

/**
 * Please keep alphabetical order.
 *
 * <p>It is quite safe to remove redundant teams from here, all the code using TwTeam has to consider it.
 *
 * <p>Functionality can be expanded later, for example for services needing a team's slack channel for automatic notifications.
 */
public enum TwTeam {
  ACTIVITY_TEAM("activity-team"),
  AML("aml"),
  ANZ("anz"),
  ASIA_CURRENCIES("asia-currencies"),
  BANKING("banking"),
  BUSINESS_CONTROLS("business-controls"),
  BUSINESS_SEND("business-send"),
  CARDS("cards"),
  COMPARISON("comparison"),
  CONNECTED_APPS("connected-apps"),
  CONTACTS("contacts"),
  CONVERSION("conversion"),
  CURRENCIES("currencies"),
  CURRENCIES_PLATFORM("currencies-platform"),
  EAST_ASIA_CURRENCIES("east-asia-currencies"),
  EU_CURRENCIES("eu-currencies"),
  FEATURE_SERVICE("feature-service"),
  FEE_CREDIT("fee-credit"),
  FINANCE("finance"),
  FRAUD_ENGINEERS("fraud-engineers"),
  GPB_TEAM("gbp-team"),
  HELP_EXPERIENCE("help-experience"),
  HOLD_TEAM("hold-team"),
  LATAM("latam"),
  M12N_CHAMPIONS("m12n_champions"),
  MEA_CURRENCIES("mea-currencies"),
  ORGANIC_GROWTH("organic-growth"),
  PAYIN_PLATFORM("payin-platform"),
  PAYMENT_VALIDATION("payment-validation"),
  PAYOUT_COORDINATOR("payout-coordinator"),
  PP_TRANSFER("pp-transfer"),
  PRICING("pricing"),
  PROFILE_SERVICE("profile-service"),
  QUOTE_SERVICE("quote-service"),
  RECEIVE("receive"),
  SECURITY_PRODUCT("security-product"),
  SECURITY("security"),
  SEND_MONEY("send-money"),
  SMB("smb"),
  TREASURY("treasury"),
  USD("usd"),
  VERIFICATION("verification"),
  VIRALITY("virality"),
  WEBAPP_RELIABILITY("webapp-reliability"),
  ;

  private static Map<String, TwTeam> githubTeamIdx = Arrays.stream(values()).collect(Collectors.toMap(TwTeam::githubTeam, Function.identity()));

  private static Map<String, TwTeam> nameIdx = Arrays.stream(values()).collect(Collectors.toMap(TwTeam::name, Function.identity()));

  public static TwTeam getByGithubTeam(String githubTeam) {
    return githubTeamIdx.get(StringUtils.lowerCase(githubTeam));
  }

  public static TwTeam getByName(String name) {
    return nameIdx.get(StringUtils.upperCase(name));
  }

  String githubTeam;

  TwTeam(String githubTeam) {
    this.githubTeam = githubTeam;
  }

  String githubTeam() {
    return githubTeam;
  }
}
