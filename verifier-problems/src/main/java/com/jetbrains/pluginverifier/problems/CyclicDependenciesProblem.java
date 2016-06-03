package com.jetbrains.pluginverifier.problems;

import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class CyclicDependenciesProblem extends Problem {

  private String myCycle;

  public CyclicDependenciesProblem() {

  }

  public CyclicDependenciesProblem(String cycle) {
    myCycle = cycle;
  }

  public String getCycle() {
    return myCycle;
  }

  public void setCycle(String cycle) {
    myCycle = cycle;
  }

  @NotNull
  @Override
  public String getDescriptionPrefix() {
    return "cyclic plugin dependencies";
  }

  @NotNull
  public String getDescription() {
    return getDescriptionPrefix() + (myCycle != null ? " " + myCycle : "");
  }

}
