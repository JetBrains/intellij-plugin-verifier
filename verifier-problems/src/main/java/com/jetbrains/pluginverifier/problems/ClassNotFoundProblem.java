package com.jetbrains.pluginverifier.problems;

import com.jetbrains.pluginverifier.utils.MessageUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ClassNotFoundProblem extends Problem {

  private String myUnknownClass;

  public ClassNotFoundProblem() {

  }

  public ClassNotFoundProblem(@NotNull String className, String unknownClass) {
    setLocation(new ProblemLocation(className));
    myUnknownClass = unknownClass;
  }

  public ClassNotFoundProblem(@NotNull String className, @NotNull String methodDescr, String unknownClass) {
    setLocation(new ProblemLocation(className, methodDescr));
    myUnknownClass = unknownClass;
  }

  public String getUnknownClass() {
    return myUnknownClass;
  }

  public void setUnknownClass(String unknownClass) {
    myUnknownClass = unknownClass;
  }

  @Override
  public String getDescription() {
    return "accessing to unknown class: " + MessageUtils.convertClassName(myUnknownClass) + " (from " + getLocation() + ')';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;

    ClassNotFoundProblem problem = (ClassNotFoundProblem)o;

    if (myUnknownClass != null ? !myUnknownClass.equals(problem.myUnknownClass) : problem.myUnknownClass != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (myUnknownClass != null ? myUnknownClass.hashCode() : 0);
    return result;
  }
}
