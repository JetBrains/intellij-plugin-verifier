package com.jetbrains.pluginverifier.problems;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This problem is not saved to report-file: it's only shown on verification status page.
 *
 * @author Sergey Patrikeev
 */
public class VerificationProblem extends Problem {

  private String myPlugin;
  private String myDetails;

  public VerificationProblem() {

  }

  public VerificationProblem(@NotNull String details, @Nullable String plugin) {
    myPlugin = plugin;
    myDetails = details;
  }

  @NotNull
  @Override
  public String getDescriptionPrefix() {
    return "failed to verify plugin";
  }

  @NotNull
  @Override
  public String getDescription() {
    return String.format("%s %s: %s", getDescriptionPrefix(), (myPlugin != null ? myPlugin : ""), myDetails);
  }

}
