package com.jetbrains.pluginverifier.problems.fields;

import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;
import com.jetbrains.pluginverifier.problems.Problem;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ChangeFinalFieldProblem extends Problem {

  @SerializedName("field")
  private String myField;

  public ChangeFinalFieldProblem() {

  }

  public ChangeFinalFieldProblem(@NotNull String field) {
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
    return "attempt to change a final field" + " " + myField;
  }

}
