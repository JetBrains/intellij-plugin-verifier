package com.jetbrains.pluginverifier.problems.statics;

import com.jetbrains.pluginverifier.problems.Problem;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by Sergey Patrikeev
 */
@XmlRootElement
public class InstanceAccessOfStaticFieldProblem extends Problem {

  private String myField;

  public InstanceAccessOfStaticFieldProblem() {
  }

  public InstanceAccessOfStaticFieldProblem(String field) {
    myField = field;
  }

  @Override
  public String getDescriptionPrefix() {
    return "attempt to perform instance access on a static field";
  }
  public String getField() {
    return myField;
  }

  public void setField(String field) {
    myField = field;
  }

  @Override
  public String getDescription() {
    return getDescriptionPrefix() + " " + myField;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    InstanceAccessOfStaticFieldProblem that = (InstanceAccessOfStaticFieldProblem) o;

    return myField != null ? myField.equals(that.myField) : that.myField == null;

  }

  @Override
  public int hashCode() {
    int result = -1;
    result = 31 * result + (myField != null ? myField.hashCode() : 0);
    return result;
  }
}
