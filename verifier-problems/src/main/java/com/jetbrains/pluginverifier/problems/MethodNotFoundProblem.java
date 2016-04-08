package com.jetbrains.pluginverifier.problems;

import com.jetbrains.pluginverifier.utils.MessageUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class MethodNotFoundProblem extends Problem {

  private String myMethodDescriptor;

  public MethodNotFoundProblem() {

  }

  public MethodNotFoundProblem(@NotNull String methodDescriptor) {
    myMethodDescriptor = methodDescriptor;
  }

  public String getMethodDescriptor() {
    return myMethodDescriptor;
  }

  public void setMethodDescriptor(String methodDescriptor) {
    myMethodDescriptor = methodDescriptor;
  }

  @Override
  public String getDescriptionPrefix() {
    return "invoking unknown method";
  }

  public String getDescription() {
    return getDescriptionPrefix() + " " + MessageUtils.convertMethodDescr(myMethodDescriptor);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    MethodNotFoundProblem problem = (MethodNotFoundProblem)o;

    return !(myMethodDescriptor != null ? !myMethodDescriptor.equals(problem.myMethodDescriptor) : problem.myMethodDescriptor != null);
  }

  @Override
  public int hashCode() {
    return 130807 + (myMethodDescriptor != null ? myMethodDescriptor.hashCode() : 0);
  }
}
