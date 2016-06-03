package com.jetbrains.pluginverifier.problems;

import com.jetbrains.pluginverifier.utils.MessageUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class DuplicateClassProblem extends Problem {
  private String myMoniker;

  private String myClassName;

  public DuplicateClassProblem() {

  }

  public DuplicateClassProblem(@NotNull String className, @NotNull String moniker) {
    myMoniker = moniker;
    myClassName = className;
  }

  public String getMoniker() {
    return myMoniker;
  }

  public void setMoniker(String moniker) {
    myMoniker = moniker;
  }

  public String getClassName() {
    return myClassName;
  }

  public void setClassName(String className) {
    myClassName = className;
  }

  @NotNull
  @Override
  public String getDescriptionPrefix() {
    return "duplicated class";
  }

  @NotNull
  public String getDescription() {
    return getDescriptionPrefix() + " (className=" + MessageUtils.convertClassName(myClassName) + " location=" + myMoniker + ")";
  }

}
