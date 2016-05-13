package com.jetbrains.pluginverifier.problems;

import com.jetbrains.pluginverifier.utils.MessageUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class InterfaceInstantiationProblem extends Problem {

  private String myClassName;

  public InterfaceInstantiationProblem() {

  }

  public InterfaceInstantiationProblem(@NotNull String className) {
    myClassName = className;
  }

  public String getClassName() {
    return myClassName;
  }

  public void setClassName(String className) {
    myClassName = className;
  }

  @Override
  public String getDescriptionPrefix() {
    return "instantiation an interface";
  }

  public String getDescription() {
    return getDescriptionPrefix() + " " + MessageUtils.convertMethodDescr(myClassName);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof InterfaceInstantiationProblem)) return false;

    InterfaceInstantiationProblem problem = (InterfaceInstantiationProblem) o;

    return !(myClassName != null ? !myClassName.equals(problem.myClassName) : problem.myClassName != null);
  }

  @Override
  public int hashCode() {
    return 2118 + (myClassName != null ? myClassName.hashCode() : 0);
  }
}
