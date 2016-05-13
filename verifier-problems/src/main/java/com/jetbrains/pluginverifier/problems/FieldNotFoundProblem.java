package com.jetbrains.pluginverifier.problems;

import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class FieldNotFoundProblem extends Problem {

  private String myField;

  public FieldNotFoundProblem() {

  }

  public FieldNotFoundProblem(@NotNull String field) {
    myField = field;
  }

  public String getField() {
    return myField;
  }

  public void setField(String field) {
    myField = field;
  }

  @Override
  public String getDescriptionPrefix() {
    return "accessing to unknown field";
  }

  public String getDescription() {
    return getDescriptionPrefix() + " " + myField;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    FieldNotFoundProblem problem = (FieldNotFoundProblem) o;

    return !(myField != null ? !myField.equals(problem.myField) : problem.myField != null);
  }

  @Override
  public int hashCode() {
    return 807130 + (myField != null ? myField.hashCode() : 0);
  }
}
