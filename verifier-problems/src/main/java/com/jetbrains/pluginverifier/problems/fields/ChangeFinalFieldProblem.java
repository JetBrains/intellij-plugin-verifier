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

  @Override
  public String getDescriptionPrefix() {
    return "attempt to change a final field";
  }

  public String getDescription() {
    return getDescriptionPrefix() + " " + myField;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ChangeFinalFieldProblem)) return false;

    ChangeFinalFieldProblem problem = (ChangeFinalFieldProblem) o;

    return !(myField != null ? !myField.equals(problem.myField) : problem.myField != null);
  }

  @Override
  public int hashCode() {
    return myField != null ? myField.hashCode() : 0;
  }
}
