package com.jetbrains.pluginverifier.problems;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by Sergey Patrikeev
 */
@XmlRootElement
public class InvokeStaticOnInstanceMethodProblem extends Problem {

  private String myMethod;

  public InvokeStaticOnInstanceMethodProblem() {
  }

  public InvokeStaticOnInstanceMethodProblem(String method) {
    myMethod = method;
  }

  @Override
  public String getDescriptionPrefix() {
    return "attempt to perform 'invokestatic' on an instance method";
  }

  public String getMethod() {
    return myMethod;
  }

  public void setMethod(String method) {
    myMethod = method;
  }

  @Override
  public String getDescription() {
    return getDescriptionPrefix() + " " + myMethod;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    InvokeStaticOnInstanceMethodProblem that = (InvokeStaticOnInstanceMethodProblem) o;

    return myMethod != null ? myMethod.equals(that.myMethod) : that.myMethod == null;

  }

  @Override
  public int hashCode() {
    int result = 32145;
    result = 31 * result + (myMethod != null ? myMethod.hashCode() : 0);
    return result;
  }
}
