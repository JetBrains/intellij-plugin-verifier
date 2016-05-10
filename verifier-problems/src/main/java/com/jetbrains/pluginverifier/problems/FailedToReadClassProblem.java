package com.jetbrains.pluginverifier.problems;

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

  @Override
  public String getDescriptionPrefix() {
    return "failed to read a class-file";
  }

  @Override
  public String getDescription() {
    return getDescriptionPrefix() + " " + myClassName + (myDetails != null ? " " + myDetails : "");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    FailedToReadClassProblem that = (FailedToReadClassProblem) o;

    if (myClassName != null ? !myClassName.equals(that.myClassName) : that.myClassName != null) return false;
    return myDetails != null ? myDetails.equals(that.myDetails) : that.myDetails == null;

  }

  @Override
  public int hashCode() {
    int result = 12431231;
    result = 31 * result + (myClassName != null ? myClassName.hashCode() : 0);
    result = 31 * result + (myDetails != null ? myDetails.hashCode() : 0);
    return result;
  }
}
