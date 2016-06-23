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
public class IllegalFieldAccessProblem extends Problem {

  private String myField;
  private AccessType myFieldAccess;

  public IllegalFieldAccessProblem() {
  }

  public IllegalFieldAccessProblem(@NotNull String field, @NotNull AccessType fieldAccess) {
    Preconditions.checkNotNull(field);
    Preconditions.checkNotNull(fieldAccess);
    myField = field;
    myFieldAccess = fieldAccess;
  }

  @NotNull
  public String getDescription() {
    return "illegal access of" + " " + myFieldAccess.getDescription() + " field " + myField;
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

  @NotNull
  @Override
  public Problem deserialize(@NotNull String... params) {
    return new IllegalFieldAccessProblem(params[0], AccessType.valueOf(params[1].toUpperCase()));
  }

  @NotNull
  @Override
  public List<Pair<String, String>> serialize() {
    //noinspection unchecked
    return Arrays.asList(new Pair<String, String>("field", myField), new Pair<String, String>("access", myFieldAccess != null ? myFieldAccess.name() : null)); //name() used because of Javadoc about stability
  }
}
