package com.jetbrains.pluginverifier.problems;

import com.google.common.base.Preconditions;
import com.jetbrains.pluginverifier.utils.Pair;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Arrays;
import java.util.List;

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

  @Override
  public Problem deserialize(String... params) {
    return new IllegalMethodAccessProblem(params[0], AccessType.valueOf(params[1].toUpperCase()));
  }

  @Override
  public List<Pair<String, String>> serialize() {
    //noinspection unchecked
    return Arrays.asList(Pair.create("method", myMethod), Pair.create("access", myMethodAccess != null ? myMethodAccess.name() : null));
  }

}
