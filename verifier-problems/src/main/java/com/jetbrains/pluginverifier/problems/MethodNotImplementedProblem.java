package com.jetbrains.pluginverifier.problems;

import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class MethodNotImplementedProblem extends Problem {

  private String myMethod;

  public MethodNotImplementedProblem() {

  }

  public MethodNotImplementedProblem(@NotNull String className, @NotNull String method) {
    setLocation(new ProblemLocation(className));
    myMethod = method;
  }

  public String getMethod() {
    return myMethod;
  }

  public void setMethod(String method) {
    myMethod = method;
  }

  @Override
  public String getDescription() {
    return "method isn't implemented: " + getLocation();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof MethodNotImplementedProblem)) return false;
    if (!super.equals(o)) return false;

    MethodNotImplementedProblem problem = (MethodNotImplementedProblem)o;

    if (myMethod != null ? !myMethod.equals(problem.myMethod) : problem.myMethod != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (myMethod != null ? myMethod.hashCode() : 0);
    return result;
  }
}
