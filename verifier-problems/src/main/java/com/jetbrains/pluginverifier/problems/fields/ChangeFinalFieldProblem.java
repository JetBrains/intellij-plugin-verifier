package com.jetbrains.pluginverifier.problems.fields;

import com.google.common.base.Preconditions;
import com.jetbrains.pluginverifier.problems.Problem;
import kotlin.Pair;
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
    return "attempt to change a final field" + " " + myField;
  }

  @NotNull
  @Override
  public Problem deserialize(@NotNull String... params) {
    return new ChangeFinalFieldProblem(params[0]);
  }

  @NotNull
  @Override
  public List<Pair<String, String>> serialize() {
    return Collections.singletonList(new Pair<String, String>("field", myField));
  }


}
