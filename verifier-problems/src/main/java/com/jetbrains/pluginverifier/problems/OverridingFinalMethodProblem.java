package com.jetbrains.pluginverifier.problems;

import com.jetbrains.pluginverifier.utils.MessageUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class OverridingFinalMethodProblem extends Problem {

  private String myMethod;

  public OverridingFinalMethodProblem() {

  }

  public OverridingFinalMethodProblem(@NotNull String method) {
    myMethod = method;
  }

  public String getMethod() {
    return myMethod;
  }

  public void setMethod(String method) {
    myMethod = method;
  }

  @Override
  public String getDescriptionPrefix() {
    return "overriding final method";
  }

  public String getDescription() {
    return getDescriptionPrefix() + " " + MessageUtils.convertMethodDescr(myMethod);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof OverridingFinalMethodProblem)) return false;

    OverridingFinalMethodProblem problem = (OverridingFinalMethodProblem)o;

    return !(myMethod != null ? !myMethod.equals(problem.myMethod) : problem.myMethod != null);
  }

  @Override
  public int hashCode() {
    return myMethod != null ? myMethod.hashCode() : 0;
  }
}
