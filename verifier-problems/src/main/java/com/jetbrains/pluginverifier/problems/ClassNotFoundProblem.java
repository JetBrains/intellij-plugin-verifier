package com.jetbrains.pluginverifier.problems;

import com.jetbrains.pluginverifier.utils.MessageUtils;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ClassNotFoundProblem extends Problem {

  private String myUnknownClass;

  public ClassNotFoundProblem() {

  }

  public ClassNotFoundProblem(String unknownClass) {
    myUnknownClass = unknownClass;
  }

  public String getUnknownClass() {
    return myUnknownClass;
  }

  public void setUnknownClass(String unknownClass) {
    myUnknownClass = unknownClass;
  }

  @Override
  public String getDescriptionPrefix() {
    return "accessing to unknown class";
  }

  @Override
  public String getDescription() {
    return getDescriptionPrefix() + " " + MessageUtils.convertClassName(myUnknownClass);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ClassNotFoundProblem problem = (ClassNotFoundProblem)o;

    return !(myUnknownClass != null ? !myUnknownClass.equals(problem.myUnknownClass) : problem.myUnknownClass != null);
  }

  @Override
  public int hashCode() {
    return 12345 + (myUnknownClass != null ? myUnknownClass.hashCode() : 0);
  }
}
