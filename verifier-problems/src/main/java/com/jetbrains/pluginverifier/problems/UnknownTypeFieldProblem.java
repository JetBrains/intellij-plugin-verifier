package com.jetbrains.pluginverifier.problems;

import com.jetbrains.pluginverifier.problems.ClassProblem;
import com.jetbrains.pluginverifier.utils.MessageUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class UnknownTypeFieldProblem extends ClassProblem {

  private String myFieldName;
  private String myFieldType;

  public UnknownTypeFieldProblem() {

  }

  public UnknownTypeFieldProblem(@NotNull String className, String fieldName, String fieldType) {
    super(className);
    myFieldName = fieldName;
    myFieldType = fieldType;
  }

  @Override
  public String getDescription() {
    return "field has unknown type: " + getClassNameHuman() + '.' + myFieldName + " type: " + MessageUtils.convertClassName(myFieldType);
  }

  @NotNull
  @Override
  public String evaluateUID() {
    return evaluateUID(myFieldName, myFieldType, getClassName());
  }

  public String getFieldName() {
    return myFieldName;
  }

  public void setFieldName(String fieldName) {
    myFieldName = fieldName;
    cleanUid();
  }

  public String getFieldType() {
    return myFieldType;
  }

  public void setFieldType(String fieldType) {
    myFieldType = fieldType;
    cleanUid();
  }
}
