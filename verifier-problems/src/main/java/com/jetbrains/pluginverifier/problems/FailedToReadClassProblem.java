package com.jetbrains.pluginverifier.problems;

import com.jetbrains.pluginverifier.problems.ClassProblem;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class FailedToReadClassProblem extends ClassProblem {

  public FailedToReadClassProblem() {

  }

  public FailedToReadClassProblem(@NotNull String className) {
    super(className);
  }

  @Override
  public String getDescription() {
    return "failed to read class: " + getClassNameHuman();
  }

  @NotNull
  @Override
  public String evaluateUID() {
    return evaluateUID(getClassName());
  }
}
