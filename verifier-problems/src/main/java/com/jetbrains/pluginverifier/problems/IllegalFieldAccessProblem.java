package com.jetbrains.pluginverifier.problems;

import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Sergey Patrikeev
 */
@XmlRootElement
public class IllegalFieldAccessProblem extends Problem {

  private String myField;
  private AccessType myFieldAccess;

  public IllegalFieldAccessProblem() {
  }

  public IllegalFieldAccessProblem(@NotNull String field, @NotNull AccessType fieldAccess) {
    myField = field;
    myFieldAccess = fieldAccess;
  }

  @NotNull
  @Override
  public String getDescriptionPrefix() {
    return "illegal access of";
  }

  @NotNull
  public String getDescription() {
    return getDescriptionPrefix() + " " + myFieldAccess.getDescription() + " field " + myField;
  }

  public String getField() {
    return myField;
  }

  public void setField(String field) {
    myField = field;
  }

  public AccessType getFieldAccess() {
    return myFieldAccess;
  }

  public void setFieldAccess(AccessType fieldAccess) {
    myFieldAccess = fieldAccess;
  }

}
