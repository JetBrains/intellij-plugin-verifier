package com.jetbrains.pluginverifier.problems;

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

  @Override
  public String getDescriptionPrefix() {
    return "cyclic plugin dependencies";
  }

  public String getDescription() {
    return getDescriptionPrefix() + (myCycle != null ? " " + myCycle : "");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    CyclicDependenciesProblem that = (CyclicDependenciesProblem) o;

    return myCycle != null ? myCycle.equals(that.myCycle) : that.myCycle == null;

  }

  @Override
  public int hashCode() {
    int result = 91827364;
    result = 31 * result + (myCycle != null ? myCycle.hashCode() : 0);
    return result;
  }
}
