package com.jetbrains.pluginverifier.problems;

import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;


public class FieldNotFoundProblem extends Problem {

  @SerializedName("field")
  private String myField;

  public FieldNotFoundProblem() {

  }

  public FieldNotFoundProblem(@NotNull String field) {
    Preconditions.checkNotNull(field);
    myField = field;
  }

  public String getField() {
    return myField;
  }

  public void setField(String field) {
    myField = field;
  }

  @NotNull
  public String getDescription() {
    return "accessing to unknown field" + " " + myField;
  }

}
