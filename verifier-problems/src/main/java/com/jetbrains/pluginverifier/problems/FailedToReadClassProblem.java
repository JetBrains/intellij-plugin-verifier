package com.jetbrains.pluginverifier.problems;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class FailedToReadClassProblem extends Problem {

  private String myClassName;

  public FailedToReadClassProblem() {

  }

  public FailedToReadClassProblem(String className) {
    myClassName = className;
  }

  @Override
  public String getDescriptionPrefix() {
    return "failed to read class";
  }

  @Override
  public String getDescription() {
    return getDescriptionPrefix() + " " + myClassName;
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof FailedToReadClassProblem;
  }

  @Override
  public int hashCode() {
    return 24111985;
  }
}
