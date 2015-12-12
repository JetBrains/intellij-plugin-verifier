package com.jetbrains.pluginverifier.problems;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Sergey Patrikeev
 */
//TODO: implement this (e.g. in case of class -> interface, or backward)
@XmlRootElement
public class IncompatibleClassChangeProblem extends Problem {

  private String myClassName;

  public IncompatibleClassChangeProblem() {
  }

  public IncompatibleClassChangeProblem(String className) {
    myClassName = className;
  }

  @Override
  public String getDescriptionPrefix() {
    return null;
  }

  @Override
  public String getDescription() {
    return null;
  }

  public String getClassName() {
    return myClassName;
  }

  public void setClassName(String className) {
    myClassName = className;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    IncompatibleClassChangeProblem that = (IncompatibleClassChangeProblem) o;

    return myClassName != null ? myClassName.equals(that.myClassName) : that.myClassName == null;

  }

  @Override
  public int hashCode() {
    int result = 12345;
    result = 31 * result + (myClassName != null ? myClassName.hashCode() : 0);
    return result;
  }
}
