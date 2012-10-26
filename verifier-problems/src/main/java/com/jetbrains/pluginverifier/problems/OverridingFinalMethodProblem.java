package com.jetbrains.pluginverifier.problems;

import com.jetbrains.pluginverifier.problems.MethodProblem;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class OverridingFinalMethodProblem extends MethodProblem {

  public OverridingFinalMethodProblem() {

  }

  public OverridingFinalMethodProblem(@NotNull String className, @NotNull String methodDescr) {
    super(className, methodDescr);
  }

  @Override
  public String getDescription() {
    return "overriding final method: " + getMethodDescrHuman();
  }

  @NotNull
  @Override
  public String evaluateUID() {
    return evaluateUID(getMethodDescr());
  }
}
