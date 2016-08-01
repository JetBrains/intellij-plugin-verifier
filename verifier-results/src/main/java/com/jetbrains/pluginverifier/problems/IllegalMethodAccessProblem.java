package com.jetbrains.pluginverifier.problems;

import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Sergey Patrikeev
 */
@XmlRootElement
public class IllegalMethodAccessProblem extends Problem {

  @SerializedName("method")
  private String myMethod;

  @SerializedName("access")
  private AccessType myMethodAccess;

  public IllegalMethodAccessProblem() {
  }

  public IllegalMethodAccessProblem(@NotNull String method, @NotNull AccessType methodAccess) {
    Preconditions.checkNotNull(method);
    Preconditions.checkNotNull(methodAccess);
    myMethod = method;
    myMethodAccess = methodAccess;
  }

  @NotNull
  public String getDescription() {
    return "illegal invocation of" + " " + myMethodAccess.getDescription() + " method " + myMethod;
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
