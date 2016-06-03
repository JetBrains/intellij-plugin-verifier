package com.jetbrains.pluginverifier.problems.fields;

import com.jetbrains.pluginverifier.problems.Problem;
import com.jetbrains.pluginverifier.utils.Pair;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Collections;
import java.util.List;

@XmlRootElement
public class ChangeFinalFieldProblem extends Problem {

  private String myField;

  public ChangeFinalFieldProblem() {

  }

  public ChangeFinalFieldProblem(@NotNull String field) {
    myField = field;
  }

  public String getField() {
    return myField;
  }

  public void setField(String field) {
    myField = field;
  }

  @NotNull
  @Override
  public String getDescriptionPrefix() {
    return "attempt to change a final field";
  }

  @NotNull
  public String getDescription() {
    return getDescriptionPrefix() + " " + myField;
  }

  @Override
  public Problem deserialize(String... params) {
    return new ChangeFinalFieldProblem(params[0]);
  }

  @Override
  public List<Pair<String, String>> serialize() {
    return Collections.singletonList(Pair.create("field", myField));
  }


}
