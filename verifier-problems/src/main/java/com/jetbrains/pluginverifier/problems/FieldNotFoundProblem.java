package com.jetbrains.pluginverifier.problems;

import com.google.common.base.Preconditions;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Collections;
import java.util.List;

@XmlRootElement
public class FieldNotFoundProblem extends Problem {

  private String myField;

  public FieldNotFoundProblem() {

  }

  public FieldNotFoundProblem(@NotNull String field) {
    Preconditions.checkNotNull(field);
    myField = field;
  }

  public String getField() {
    return myField;
  }

  public void setField(String field) {
    myField = field;
  }

  @NotNull
  public String getDescription() {
    return "accessing to unknown field" + " " + myField;
  }

  @NotNull
  @Override
  public Problem deserialize(@NotNull String... params) {
    return new FieldNotFoundProblem(params[0]);
  }

  @NotNull
  @Override
  public List<Pair<String, String>> serialize() {
    return Collections.singletonList(new Pair<String, String>("field", myField));
  }

}
