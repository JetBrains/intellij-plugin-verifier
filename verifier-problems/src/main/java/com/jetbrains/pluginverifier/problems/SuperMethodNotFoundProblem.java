package com.jetbrains.pluginverifier.problems;

import com.jetbrains.pluginverifier.problems.MethodProblem;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class SuperMethodNotFoundProblem extends MethodProblem {

  public SuperMethodNotFoundProblem() {

  }

  public SuperMethodNotFoundProblem(@NotNull String className, @NotNull String methodDescr) {
    super(className, methodDescr);
  }

  @Override
  public String getDescription() {
    return "method isn't implemented: " + getMethodDescrHuman();
  }

  @NotNull
  @Override
  public String evaluateUID() {
    return evaluateUID(getMethodDescr());
  }
}
