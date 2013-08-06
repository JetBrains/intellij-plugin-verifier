package com.jetbrains.pluginverifier.problems;

import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class SuperMethodNotFoundProblem extends Problem {

  public SuperMethodNotFoundProblem() {

  }

  public SuperMethodNotFoundProblem(@NotNull String className, @NotNull String methodDescr) {
    setLocation(new ProblemLocation(className, methodDescr));
  }

  @Override
  public String getDescription() {
    return "method isn't implemented: " + getLocation();
  }
}
