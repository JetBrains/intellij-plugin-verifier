package com.jetbrains.pluginverifier.problems;

import com.google.common.base.Preconditions;
import kotlin.Pair;
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

  @NotNull
  @Override
  public Problem deserialize(@NotNull String... params) {
    return new IncompatibleClassChangeProblem(params[0], Change.valueOf(params[1].toUpperCase()));
  }

  @NotNull
  @Override
  public List<Pair<String, String>> serialize() {
    //noinspection unchecked
    return Arrays.asList(new Pair<String, String>("class", myClassName), new Pair<String, String>("change", myChange != null ? myChange.name() : null));
  }

  public enum Change {
    CLASS_TO_INTERFACE,
    INTERFACE_TO_CLASS
  }


}
