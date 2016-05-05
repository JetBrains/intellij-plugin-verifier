package com.jetbrains.pluginverifier.problems;

import com.jetbrains.pluginverifier.utils.MessageUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class AbstractClassInstantiationProblem extends Problem {

  private String myClassName;

  public AbstractClassInstantiationProblem() {

  }

  public AbstractClassInstantiationProblem(@NotNull String className) {
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
    return "instantiation an abstract class";
  }

  public String getDescription() {
    return getDescriptionPrefix() + " " + MessageUtils.convertMethodDescr(myClassName);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof AbstractClassInstantiationProblem)) return false;

    AbstractClassInstantiationProblem problem = (AbstractClassInstantiationProblem) o;

    return !(myClassName != null ? !myClassName.equals(problem.myClassName) : problem.myClassName != null);
  }

  @Override
  public int hashCode() {
    return 9002118 + (myClassName != null ? myClassName.hashCode() : 0);
  }
}
