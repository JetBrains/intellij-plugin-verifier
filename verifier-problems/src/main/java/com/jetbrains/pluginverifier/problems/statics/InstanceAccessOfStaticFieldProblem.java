package com.jetbrains.pluginverifier.problems.statics;

import com.google.common.base.Preconditions;
import com.jetbrains.pluginverifier.problems.Problem;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Collections;
import java.util.List;

/**
 * Created by Sergey Patrikeev
 */
@XmlRootElement
public class InstanceAccessOfStaticFieldProblem extends Problem {

  private String myField;

  public InstanceAccessOfStaticFieldProblem() {
  }

  public InstanceAccessOfStaticFieldProblem(@NotNull String field) {
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
  @Override
  public String getDescription() {
    return "attempt to perform instance access on a static field" + " " + myField;
  }

  @NotNull
  @Override
  public Problem deserialize(@NotNull String... params) {
    return new InstanceAccessOfStaticFieldProblem(params[0]);
  }

  @NotNull
  @Override
  public List<Pair<String, String>> serialize() {
    return Collections.singletonList(new Pair<String, String>("field", myField));
  }


}
