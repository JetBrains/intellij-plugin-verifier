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

  public void setCalledMethod(String calledMethod) {
    //for legacy deserialization (don't add getCalledMethod!)
    myMethod = calledMethod;
  }

  public void setMethod(String method) {
    //for legacy serialization (don't add getMethod)
    myMethod = method;
  }

  //serialized form of the MethodNotFoundProblem will contain "methodDescriptor"
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
