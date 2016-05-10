package com.jetbrains.pluginverifier.problems;

/**
 * @author Sergey Patrikeev
 */

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This problem is not saved to report-file: it's only shown on verification status page
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

  @Override
  public String getDescriptionPrefix() {
    return "failed to verify plugin";
  }

  @Override
  public String getDescription() {
    return String.format("%s %s: %s", getDescriptionPrefix(), (myPlugin != null ? myPlugin : ""), myDetails);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    VerificationProblem that = (VerificationProblem) o;

    //noinspection SimplifiableIfStatement
    if (myPlugin != null ? !myPlugin.equals(that.myPlugin) : that.myPlugin != null) return false;
    return myDetails != null ? myDetails.equals(that.myDetails) : that.myDetails == null;

  }

  @Override
  public int hashCode() {
    int result = 123454321;
    result = 31 * result + (myPlugin != null ? myPlugin.hashCode() : 0);
    result = 31 * result + (myDetails != null ? myDetails.hashCode() : 0);
    return result;
  }
}
