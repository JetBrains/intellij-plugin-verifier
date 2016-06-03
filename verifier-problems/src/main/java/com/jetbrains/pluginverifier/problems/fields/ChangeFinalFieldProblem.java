package com.jetbrains.pluginverifier.problems.fields;

import com.jetbrains.pluginverifier.problems.Problem;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ChangeFinalFieldProblem extends Problem {

  private String myField;

  public ChangeFinalFieldProblem() {

  }

  public ChangeFinalFieldProblem(@NotNull String field) {
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
    return "attempt to change a final field";
  }

  @NotNull
  public String getDescription() {
    return getDescriptionPrefix() + " " + myField;
  }

}
