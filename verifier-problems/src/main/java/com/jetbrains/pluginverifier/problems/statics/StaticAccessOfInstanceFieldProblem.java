package com.jetbrains.pluginverifier.problems.statics;

import com.jetbrains.pluginverifier.problems.Problem;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by Sergey Patrikeev
 */
@XmlRootElement
public class StaticAccessOfInstanceFieldProblem extends Problem {

  private String myField;

  public StaticAccessOfInstanceFieldProblem() {
  }

  public StaticAccessOfInstanceFieldProblem(String field) {
    myField = field;
  }

  @Override
  public String getDescriptionPrefix() {
    return "attempt to perform static access on an instance field";
  }

  @Override
  public String getDescription() {
    return getDescriptionPrefix() + " " + myField;
  }

  public String getField() {
    return myField;
  }

  public void setField(String field) {
    myField = field;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    StaticAccessOfInstanceFieldProblem that = (StaticAccessOfInstanceFieldProblem) o;

    return myField != null ? myField.equals(that.myField) : that.myField == null;

  }

  @Override
  public int hashCode() {
    int result = -2;
    result = 31 * result + (myField != null ? myField.hashCode() : 0);
    return result;
  }
}
