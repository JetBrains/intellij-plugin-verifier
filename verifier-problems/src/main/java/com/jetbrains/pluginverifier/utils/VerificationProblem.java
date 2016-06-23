package com.jetbrains.pluginverifier.utils;

import com.jetbrains.pluginverifier.problems.Problem;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * This problem is not saved to report-file: it's only shown on verification status page.
 *
 * @author Sergey Patrikeev
 */
//TODO: get rid of this class
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
  public String getDescription() {
    return String.format("%s %s: %s", "failed to verify plugin", (myPlugin != null ? myPlugin : ""), myDetails);
  }

  @NotNull
  @Override
  public Problem deserialize(@NotNull String... params) {
    throw new UnsupportedOperationException("The class is not intended to be serialized");
  }

  @NotNull
  @Override
  public List<Pair<String, String>> serialize() {
    throw new UnsupportedOperationException("The class is not intended to be serialized");
  }

}
