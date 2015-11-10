package com.jetbrains.pluginverifier.problems;

import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Sergey Patrikeev
 */
@XmlRootElement
public class IllegalMethodAccessProblem extends Problem {

  private String myMethod;
  private MethodAccess myMethodAccess;

  public IllegalMethodAccessProblem() {
  }

  public IllegalMethodAccessProblem(@NotNull String method, @NotNull MethodAccess methodAccess) {
    myMethod = method;
    myMethodAccess = methodAccess;
  }

  @Override
  public String getDescription() {
    return "illegal invocation of " + myMethodAccess.getDescription() + " method " + myMethod;
  }

  public String getMethod() {
    return myMethod;
  }

  public void setMethod(String method) {
    myMethod = method;
  }

  public MethodAccess getMethodAccess() {
    return myMethodAccess;
  }

  public void setMethodAccess(MethodAccess methodAccess) {
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

  public enum MethodAccess {
    PUBLIC("public"),
    PROTECTED("protected"),
    PACKAGE_PRIVATE("package-private"),
    PRIVATE("private");

    private final String myDescription;

    MethodAccess(@NotNull String description) {
      myDescription = description;
    }

    @NotNull
    public String getDescription() {
      return myDescription;
    }
  }
}
