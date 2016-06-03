package com.jetbrains.pluginverifier.problems;

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

  @NotNull
  @Override
  public String getDescriptionPrefix() {
    return "broken plugin";
  }

  @NotNull
  @Override
  public String getDescription() {
    return getDescriptionPrefix() + (myDetails != null ? " " + myDetails : "");
  }

}
