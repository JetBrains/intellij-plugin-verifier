package com.jetbrains.pluginverifier.problems;

/**
 * @author Sergey Patrikeev
 */

import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class BrokenPluginProblem extends Problem {

  private String myDetails;

  public BrokenPluginProblem() {

  }

  public BrokenPluginProblem(@NotNull String details) {
    myDetails = details;
  }

  @Override
  public String getDescriptionPrefix() {
    return "broken plugin";
  }

  @Override
  public String getDescription() {
    return getDescriptionPrefix() + (myDetails != null ? " " + myDetails : "");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    BrokenPluginProblem that = (BrokenPluginProblem) o;

    return myDetails != null ? myDetails.equals(that.myDetails) : that.myDetails == null;

  }

  @Override
  public int hashCode() {
    return myDetails.hashCode();
  }
}
