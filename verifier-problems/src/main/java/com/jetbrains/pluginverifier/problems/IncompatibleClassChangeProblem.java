package com.jetbrains.pluginverifier.problems;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Sergey Patrikeev
 */
@XmlRootElement
public class IncompatibleClassChangeProblem extends Problem {

  private String myClassName;
  private Change myChange;

  public IncompatibleClassChangeProblem() {
  }

  public IncompatibleClassChangeProblem(String className, Change change) {
    myClassName = className;
    myChange = change;
  }

  @Override
  public String getDescriptionPrefix() {
    return "incompatible change problem";
  }

  public String getDescription() {
    String s = null;
    if (myChange != null) {
      switch (myChange) {
        case CLASS_TO_INTERFACE:
          s = "(expected a class but found an interface)";
          break;
        case INTERFACE_TO_CLASS:
          s = "(expected an interface but found a class)";
          break;
      }
    }
    return getDescriptionPrefix() + " of class " + myClassName + (s != null ? " " + s : "");
  }

  public String getClassName() {
    return myClassName;
  }

  public void setClassName(String className) {
    myClassName = className;
  }

  public Change getChange() {
    return myChange;
  }

  public void setChange(Change change) {
    myChange = change;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    IncompatibleClassChangeProblem that = (IncompatibleClassChangeProblem) o;

    if (myClassName != null ? !myClassName.equals(that.myClassName) : that.myClassName != null) return false;
    return myChange == that.myChange;

  }

  @Override
  public int hashCode() {
    int result = 105032;
    result = 31 * result + (myClassName != null ? myClassName.hashCode() : 0);
    result = 31 * result + (myChange != null ? myChange.hashCode() : 0);
    return result;
  }

  public enum Change {
    CLASS_TO_INTERFACE,
    INTERFACE_TO_CLASS
  }

}
