package com.jetbrains.pluginverifier.problems;

import com.google.common.base.Preconditions;
import com.jetbrains.pluginverifier.utils.Pair;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Arrays;
import java.util.List;

@XmlRootElement
public class FailedToReadClassProblem extends Problem {

  private String myClassName;
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
  public String getDescriptionPrefix() {
    return "failed to read a class-file";
  }

  @NotNull
  @Override
  public String getDescription() {
    return getDescriptionPrefix() + " " + myClassName + (myDetails != null ? " " + myDetails : "");
  }


  @Override
  public Problem deserialize(String... params) {
    return new FailedToReadClassProblem(params[0], params[1]);
  }

  @Override
  public List<Pair<String, String>> serialize() {
    //noinspection unchecked
    return Arrays.asList(Pair.create("class", myClassName), Pair.create("details", myDetails));
  }


}
