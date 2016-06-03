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

  @NotNull
  @Override
  public String getDescriptionPrefix() {
    return "illegal invocation of";
  }

  @NotNull
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

}
