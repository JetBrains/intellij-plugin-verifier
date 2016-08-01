package com.jetbrains.pluginverifier.problems;

import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;

/**
 * @author Sergey Patrikeev
 */

public class IncompatibleClassChangeProblem extends Problem {

  @SerializedName("class")
  private String myClassName;

  @SerializedName("change")
  private Change myChange;

  public IncompatibleClassChangeProblem() {
  }

  public IncompatibleClassChangeProblem(@NotNull String className, @NotNull Change change) {
    Preconditions.checkNotNull(className);
    Preconditions.checkNotNull(change);
    myClassName = className;
    myChange = change;
  }

  @NotNull
  public String getDescription() {
    String s = null;
    if (myChange != null) {
      switch (myChange) {
        case CLASS_TO_INTERFACE:
          s = "(expected a class but found an interface)";
          break;
        case INTERFACE_TO_CLASS:
          s = "(expected an interface but found a class)";
          break;
      }
    }
    return "incompatible change problem" + " of class " + myClassName + (s != null ? " " + s : "");
  }

  public String getClassName() {
    return myClassName;
  }

  public void setClassName(String className) {
    myClassName = className;
  }

  public Change getChange() {
    return myChange;
  }

  public void setChange(Change change) {
    myChange = change;
  }

  public enum Change {
    CLASS_TO_INTERFACE,
    INTERFACE_TO_CLASS
  }


}
