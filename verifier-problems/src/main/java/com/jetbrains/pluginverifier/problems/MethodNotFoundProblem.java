package com.jetbrains.pluginverifier.problems;

import com.jetbrains.pluginverifier.utils.MessageUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class MethodNotFoundProblem extends Problem {

  private String myCalledMethod;

  public MethodNotFoundProblem() {

  }

  public MethodNotFoundProblem(@NotNull String className, @NotNull String methodDescr, @NotNull String calledMethod) {
    setLocation(new ProblemLocation(className, methodDescr));
    myCalledMethod = calledMethod;
  }

  public String getCalledMethod() {
    return myCalledMethod;
  }

  public void setCalledMethod(String calledMethod) {
    myCalledMethod = calledMethod;
  }

  @Override
  public String getDescription() {
    return "invoking unknown method: " + MessageUtils.convertMethodDescr(myCalledMethod) + " (from " + getLocation() + ')';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;

    MethodNotFoundProblem problem = (MethodNotFoundProblem)o;

    if (myCalledMethod != null ? !myCalledMethod.equals(problem.myCalledMethod) : problem.myCalledMethod != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (myCalledMethod != null ? myCalledMethod.hashCode() : 0);
    return result;
  }
}
