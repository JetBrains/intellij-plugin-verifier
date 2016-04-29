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

  @Override
  public String getDescriptionPrefix() {
    return "illegal access of";
  }

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

  @SuppressWarnings("SimplifiableIfStatement")
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof IllegalFieldAccessProblem)) return false;

    IllegalFieldAccessProblem that = (IllegalFieldAccessProblem) o;

    if (myField != null ? !myField.equals(that.myField) : that.myField != null) return false;
    return myFieldAccess == that.myFieldAccess;

  }

  @Override
  public int hashCode() {
    int result = 100501;
    result = 31 * result + (myField != null ? myField.hashCode() : 0);
    result = 31 * result + (myFieldAccess != null ? myFieldAccess.hashCode() : 0);
    return result;
  }

}
