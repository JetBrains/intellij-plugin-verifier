package com.jetbrains.pluginverifier.problems;

import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class FailedToReadClassProblem extends Problem {

  private String myClassName;
  private String myDetails;

  public FailedToReadClassProblem() {
  }

  public FailedToReadClassProblem(String className, String details) {
    myClassName = className;
    myDetails = details;
  }

  public String getClassName() {
    return myClassName;
  }

  public void setClassName(String className) {
    myClassName = className;
  }

  public String getDetails() {
    return myDetails;
  }

  public void setDetails(String details) {
    myDetails = details;
  }

  @NotNull
  @Override
  public String getDescriptionPrefix() {
    return "failed to read a class-file";
  }

  @NotNull
  @Override
  public String getDescription() {
    return getDescriptionPrefix() + " " + myClassName + (myDetails != null ? " " + myDetails : "");
  }

}
