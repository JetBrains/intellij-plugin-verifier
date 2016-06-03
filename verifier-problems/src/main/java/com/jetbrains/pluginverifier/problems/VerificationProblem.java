package com.jetbrains.pluginverifier.problems;

import com.jetbrains.pluginverifier.utils.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

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

  @Override
  public Problem deserialize(String... params) {
    throw new UnsupportedOperationException("The class is not intended to be serialized");
  }

  @Override
  public List<Pair<String, String>> serialize() {
    throw new UnsupportedOperationException("The class is not intended to be serialized");
  }

}
