package com.jetbrains.pluginverifier.problems;

import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;


public class FailedToReadClassProblem extends Problem {

  @SerializedName("class")
  private String myClassName;
  @SerializedName("details")
  private String myDetails;

  public FailedToReadClassProblem() {
  }

  public FailedToReadClassProblem(@NotNull String className, @NotNull String details) {
    Preconditions.checkNotNull(className);
    Preconditions.checkNotNull(details);
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
  public String getDescription() {
    return "failed to read a class-file" + " " + myClassName + (myDetails != null ? " " + myDetails : "");
  }

}
