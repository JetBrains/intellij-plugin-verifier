package com.jetbrains.pluginverifier.problems.statics;

import com.jetbrains.pluginverifier.problems.Problem;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by Sergey Patrikeev
 */
@XmlRootElement
public class InstanceAccessOfStaticFieldProblem extends Problem {

  private String myField;
  private Instruction myInstruction;

  public InstanceAccessOfStaticFieldProblem() {
  }

  public InstanceAccessOfStaticFieldProblem(String field, Instruction instruction) {
    myField = field;
    myInstruction = instruction;
  }

  @Override
  public String getDescriptionPrefix() {
    return "attempt to perform '" + myInstruction.getName() + "' on a static field";
  }

  public Instruction getInstruction() {
    return myInstruction;
  }

  public void setInstruction(Instruction instruction) {
    myInstruction = instruction;
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

    return myField != null ? myField.equals(that.myField) : that.myField == null && myInstruction == that.myInstruction;

  }

  @Override
  public int hashCode() {
    int result = -123;
    result = 31 * result + (myField != null ? myField.hashCode() : 0);
    result = 31 * result + (myInstruction != null ? myInstruction.hashCode() : 0);
    return result;
  }

  public enum Instruction {
    PUT_FIELD("putfield"),
    GET_FIELD("getfield");

    private final String myName;

    Instruction(String name) {
      myName = name;
    }

    public String getName() {
      return myName;
    }
  }
}
