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
public class IllegalFieldAccessProblem extends Problem {

  private String myField;
  private AccessType myFieldAccess;

  public IllegalFieldAccessProblem() {
  }

  public IllegalFieldAccessProblem(@NotNull String field, @NotNull AccessType fieldAccess) {
    myField = field;
    myFieldAccess = fieldAccess;
  }

  @NotNull
  @Override
  public String getDescriptionPrefix() {
    return "illegal access of";
  }

  @NotNull
  public String getDescription() {
    return getDescriptionPrefix() + " " + myFieldAccess.getDescription() + " field " + myField;
  }

  public String getField() {
    return myField;
  }

  public void setField(String field) {
    myField = field;
  }

  public AccessType getFieldAccess() {
    return myFieldAccess;
  }

  public void setFieldAccess(AccessType fieldAccess) {
    myFieldAccess = fieldAccess;
  }

  @Override
  public Problem deserialize(String... params) {
    return new IllegalFieldAccessProblem(params[0], AccessType.valueOf(params[1].toUpperCase()));
  }

  @Override
  public List<Pair<String, String>> serialize() {
    //noinspection unchecked
    return Arrays.asList(Pair.create("field", myField), Pair.create("access", myFieldAccess != null ? myFieldAccess.name() : null)); //name() used because of Javadoc about stability
  }
}
