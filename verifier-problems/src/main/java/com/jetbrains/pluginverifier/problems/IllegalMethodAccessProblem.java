package com.jetbrains.pluginverifier.problems;

import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Sergey Patrikeev
 */
@XmlRootElement
public class IllegalMethodAccessProblem extends Problem {

  private String myMethod;
  private AccessType myMethodAccess;

  public IllegalMethodAccessProblem() {
  }

  public IllegalMethodAccessProblem(@NotNull String method, @NotNull AccessType methodAccess) {
    myMethod = method;
    myMethodAccess = methodAccess;
  }

  @Override
  public String getDescriptionPrefix() {
    return "illegal invocation of";
  }

  public String getDescription() {
    return getDescriptionPrefix() + " " + myMethodAccess.getDescription() + " method " + myMethod;
  }

  public String getMethod() {
    return myMethod;
  }

  public void setMethod(String method) {
    myMethod = method;
  }

  public AccessType getMethodAccess() {
    return myMethodAccess;
  }

  public void setMethodAccess(AccessType methodAccess) {
    myMethodAccess = methodAccess;
  }

  @SuppressWarnings("SimplifiableIfStatement")
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof IllegalMethodAccessProblem)) return false;

    IllegalMethodAccessProblem that = (IllegalMethodAccessProblem) o;

    if (myMethod != null ? !myMethod.equals(that.myMethod) : that.myMethod != null) return false;
    return myMethodAccess == that.myMethodAccess;

  }

  @Override
  public int hashCode() {
    int result = 100500;
    result = 31 * result + (myMethod != null ? myMethod.hashCode() : 0);
    result = 31 * result + (myMethodAccess != null ? myMethodAccess.hashCode() : 0);
    return result;
  }

}
