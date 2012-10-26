package com.jetbrains.pluginverifier.problems;

import com.jetbrains.pluginverifier.problems.ClassProblem;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class DuplicateClassProblem extends ClassProblem {
  private String myMoniker;

  public DuplicateClassProblem() {

  }

  public DuplicateClassProblem(@NotNull String className, @NotNull String moniker) {
    super(className);
    myMoniker = moniker;
  }

  public String getMoniker() {
    return myMoniker;
  }

  public void setMoniker(String moniker) {
    myMoniker = moniker;
  }

  @Override
  public String getDescription() {
    return "duplicated class (className=" + getClassNameHuman() + " location=" + myMoniker + ")";
  }

  @NotNull
  @Override
  public String evaluateUID() {
    return evaluateUID(getClassName());
  }
}
