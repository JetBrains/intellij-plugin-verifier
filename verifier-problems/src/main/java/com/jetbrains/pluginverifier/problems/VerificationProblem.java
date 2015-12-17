package com.jetbrains.pluginverifier.problems;

/**
 * @author Sergey Patrikeev
 */

import org.jetbrains.annotations.NotNull;

/**
 * This problem is not saved to report-file: it's only shown on verification status page
 */
public class VerificationProblem extends Problem {

  private String myDescription;
  private String myDetails;

  public VerificationProblem() {
    myDescription = getDescriptionPrefix();
  }

  public VerificationProblem(@NotNull String description, @NotNull String details) {
    myDescription = description;
    myDetails = details;
  }

  @Override
  public String getDescriptionPrefix() {
    return myDescription;
  }

  @Override
  public String getDescription() {
    return getDescriptionPrefix();
  }

  public String getDetails() {
    return myDetails;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    VerificationProblem that = (VerificationProblem) o;

    return myDescription.equals(that.myDescription);

  }

  @Override
  public int hashCode() {
    return myDescription.hashCode();
  }
}
