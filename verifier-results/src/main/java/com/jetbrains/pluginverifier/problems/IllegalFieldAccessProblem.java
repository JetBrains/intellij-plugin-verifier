package com.jetbrains.pluginverifier.problems;

import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;

/**
 * @author Sergey Patrikeev
 */

public class IllegalFieldAccessProblem extends Problem {

  @SerializedName("field")
  private String myField;

  @SerializedName("access")
  private AccessType myFieldAccess;

  public IllegalFieldAccessProblem() {
  }

  public IllegalFieldAccessProblem(@NotNull String field, @NotNull AccessType fieldAccess) {
    Preconditions.checkNotNull(field);
    Preconditions.checkNotNull(fieldAccess);
    myField = field;
    myFieldAccess = fieldAccess;
  }

  @NotNull
  public String getDescription() {
    return "illegal access of" + " " + myFieldAccess.getDescription() + " field " + myField;
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
