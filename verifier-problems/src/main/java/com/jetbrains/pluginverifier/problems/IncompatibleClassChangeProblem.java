package com.jetbrains.pluginverifier.problems;

import com.jetbrains.pluginverifier.utils.Pair;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Arrays;
import java.util.List;

/**
 * @author Sergey Patrikeev
 */
@XmlRootElement
public class IncompatibleClassChangeProblem extends Problem {

  private String myClassName;
  private Change myChange;

  public IncompatibleClassChangeProblem() {
  }

  public IncompatibleClassChangeProblem(@NotNull String className, @NotNull Change change) {
    myClassName = className;
    myChange = change;
  }

  @NotNull
  @Override
  public String getDescriptionPrefix() {
    return "incompatible change problem";
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
    return getDescriptionPrefix() + " of class " + myClassName + (s != null ? " " + s : "");
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

  @Override
  public Problem deserialize(String... params) {
    return new IncompatibleClassChangeProblem(params[0], Change.valueOf(params[1].toUpperCase()));
  }

  @Override
  public List<Pair<String, String>> serialize() {
    //noinspection unchecked
    return Arrays.asList(Pair.create("class", myClassName), Pair.create("change", myChange != null ? myChange.name() : null));
  }

  public enum Change {
    CLASS_TO_INTERFACE,
    INTERFACE_TO_CLASS
  }


}
