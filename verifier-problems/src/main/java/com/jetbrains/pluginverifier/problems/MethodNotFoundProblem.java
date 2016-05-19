package com.jetbrains.pluginverifier.problems;

import com.jetbrains.pluginverifier.utils.MessageUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class MethodNotFoundProblem extends Problem {

  private String myMethod;

  public MethodNotFoundProblem() {

  }

  public MethodNotFoundProblem(@NotNull String method) {
    myMethod = method;
  }

  public String getCalledMethod() {
    //for legacy serialization
    return myMethod;
  }

  public void setCalledMethod(String calledMethod) {
    //for legacy serialization
    myMethod = calledMethod;
  }

  public String getMethodDescriptor() {
    return myMethod;
  }

  public void setMethodDescriptor(String methodDescriptor) {
    myMethod = methodDescriptor;
  }

  @Override
  public String getDescriptionPrefix() {
    return "invoking unknown method";
  }

  public String getDescription() {
    return getDescriptionPrefix() + " " + MessageUtils.convertMethodDescr(myMethod);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    MethodNotFoundProblem problem = (MethodNotFoundProblem)o;

    return !(myMethod != null ? !myMethod.equals(problem.myMethod) : problem.myMethod != null);
  }

  @Override
  public int hashCode() {
    return 130807 + (myMethod != null ? myMethod.hashCode() : 0);
  }
}
