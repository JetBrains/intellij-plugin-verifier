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
    setLocation(new ProblemLocation());
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
    return "overriding final method: " + MessageUtils.convertMethodDescr(myMethod);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof OverridingFinalMethodProblem)) return false;
    if (!super.equals(o)) return false;

    OverridingFinalMethodProblem problem = (OverridingFinalMethodProblem)o;

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
