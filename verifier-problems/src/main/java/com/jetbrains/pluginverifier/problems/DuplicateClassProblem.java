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

  @Override
  public String getDescriptionPrefix() {
    return "duplicated class";
  }

  @Override
  public String getDescription() {
    return getDescriptionPrefix() + " (className=" + MessageUtils.convertClassName(myClassName) + " location=" + myMoniker + ")";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    DuplicateClassProblem problem = (DuplicateClassProblem)o;

    //noinspection SimplifiableIfStatement
    if (myClassName != null ? !myClassName.equals(problem.myClassName) : problem.myClassName != null) return false;
    return myMoniker != null ? myMoniker.equals(problem.myMoniker) : problem.myMoniker == null;

  }

  @Override
  public int hashCode() {
    int result = myMoniker != null ? myMoniker.hashCode() : 0;
    result = 31 * result + (myClassName != null ? myClassName.hashCode() : 0);
    return result;
  }
}
