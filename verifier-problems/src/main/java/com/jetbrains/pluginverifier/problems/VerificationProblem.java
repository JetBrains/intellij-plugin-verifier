package com.jetbrains.pluginverifier.problems;

/**
 * @author Sergey Patrikeev
 */

import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * This problem is not saved to report-file: it's only shown on verification status page
 */
@XmlRootElement
public class VerificationProblem extends Problem {

  private final String myDescription;

  public VerificationProblem() {
    myDescription = getDescriptionPrefix() + " (see build log for details)";
  }

  public VerificationProblem(@NotNull String description) {
    myDescription = description;
  }

  @Override
  public String getDescriptionPrefix() {
    return "Unknown verification problem";
  }

  @Override
  public String getDescription() {
    return myDescription;
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
