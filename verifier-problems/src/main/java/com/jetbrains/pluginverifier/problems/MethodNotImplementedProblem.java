package com.jetbrains.pluginverifier.problems;

import com.jetbrains.pluginverifier.utils.MessageUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class MethodNotImplementedProblem extends Problem {

  private String myMethod;

  public MethodNotImplementedProblem() {

  }

  public MethodNotImplementedProblem(@NotNull String method) {
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
    return "method isn't implemented: " + MessageUtils.convertMethodDescr(myMethod);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof MethodNotImplementedProblem)) return false;

    MethodNotImplementedProblem problem = (MethodNotImplementedProblem)o;

    return !(myMethod != null ? !myMethod.equals(problem.myMethod) : problem.myMethod != null);
  }

  @Override
  public int hashCode() {
    return 8112009 + (myMethod != null ? myMethod.hashCode() : 0);
  }
}
