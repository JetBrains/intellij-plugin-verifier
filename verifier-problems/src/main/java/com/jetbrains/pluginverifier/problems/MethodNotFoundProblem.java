package com.jetbrains.pluginverifier.problems;

import com.jetbrains.pluginverifier.utils.MessageUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class MethodNotFoundProblem extends Problem {

  private String myCalledMethod;

  public MethodNotFoundProblem() {

  }

  public MethodNotFoundProblem(@NotNull String calledMethod) {
    myCalledMethod = calledMethod;
  }

  public String getCalledMethod() {
    return myCalledMethod;
  }

  public void setCalledMethod(String calledMethod) {
    myCalledMethod = calledMethod;
  }

  @Override
  public String getDescriptionPrefix() {
    return "invoking unknown method";
  }

  public String getDescription() {
    return getDescriptionPrefix() + " " + MessageUtils.convertMethodDescr(myCalledMethod);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    MethodNotFoundProblem problem = (MethodNotFoundProblem)o;

    return !(myCalledMethod != null ? !myCalledMethod.equals(problem.myCalledMethod) : problem.myCalledMethod != null);
  }

  @Override
  public int hashCode() {
    return 130807 + (myCalledMethod != null ? myCalledMethod.hashCode() : 0);
  }
}
