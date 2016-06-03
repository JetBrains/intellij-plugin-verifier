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

  @NotNull
  @Override
  public String getDescriptionPrefix() {
    return "accessing to unknown field";
  }

  @NotNull
  public String getDescription() {
    return getDescriptionPrefix() + " " + myField;
  }

}
