package com.jetbrains.pluginverifier.problems.statics;

import com.jetbrains.pluginverifier.problems.Problem;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by Sergey Patrikeev
 */
@XmlRootElement
public class StaticAccessOfInstanceFieldProblem extends Problem {

  private String myField;
  private Instruction myInstruction;

  public StaticAccessOfInstanceFieldProblem() {
  }

  public StaticAccessOfInstanceFieldProblem(String field, Instruction instruction) {
    myField = field;
    myInstruction = instruction;
  }

  @Override
  public String getDescriptionPrefix() {
    return "attempt to perform '" + myInstruction.getName() + "' on an instance field";
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

  public Instruction getInstruction() {
    return myInstruction;
  }

  public void setInstruction(Instruction instruction) {
    myInstruction = instruction;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    StaticAccessOfInstanceFieldProblem that = (StaticAccessOfInstanceFieldProblem) o;

    return myField != null ? myField.equals(that.myField) : that.myField == null && myInstruction == that.myInstruction;

  }

  @Override
  public int hashCode() {
    int result = 321;
    result = 31 * result + (myField != null ? myField.hashCode() : 0);
    result = 31 * result + (myInstruction != null ? myInstruction.hashCode() : 0);
    return result;
  }

  public enum Instruction {
    PUT_STATIC("putstatic"),
    GET_STATIC("getstatic");

    private final String myName;

    Instruction(String name) {
      myName = name;
    }

    public String getName() {
      return myName;
    }
  }

}
