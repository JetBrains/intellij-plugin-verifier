package com.jetbrains.pluginverifier.problems;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Sergey Patrikeev
 */
@XmlRootElement
public class IllegalMethodAccessProblem extends Problem {

  private String myMethod;

  public IllegalMethodAccessProblem() {
  }

  public IllegalMethodAccessProblem(String method) {
    myMethod = method;
  }

  @Override
  public String getDescription() {
    return "illegal invocation (access modifier changed) of method " + myMethod;
  }

  public String getMethod() {
    return myMethod;
  }

  public void setMethod(String method) {
    myMethod = method;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof IllegalMethodAccessProblem)) return false;

    IllegalMethodAccessProblem that = (IllegalMethodAccessProblem) o;

    return !(myMethod != null ? !myMethod.equals(that.myMethod) : that.myMethod != null);

  }

  @Override
  public int hashCode() {
    return 2015 * (myMethod != null ? myMethod.hashCode() : 0);
  }
}
