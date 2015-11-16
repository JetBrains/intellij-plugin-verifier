package com.jetbrains.pluginverifier.problems;

/**
 * @author Sergey Patrikeev
 */

import org.jetbrains.annotations.NotNull;

/**
 * This problem is not saved to report-file: it's only shown on verification status page
 */
public class VerificationProblem extends Problem {

  private final String myDescription;

  public VerificationProblem() {
    myDescription = "Unknown verification problem (see build log for details)";
  }

  public VerificationProblem(@NotNull String description) {
    myDescription = description;
  }

  @Override
  public String getDescription() {
    return myDescription;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;

    VerificationProblem that = (VerificationProblem) o;

    return myDescription.equals(that.myDescription);

  }

  @Override
  public int hashCode() {
    return myDescription.hashCode();
  }
}
