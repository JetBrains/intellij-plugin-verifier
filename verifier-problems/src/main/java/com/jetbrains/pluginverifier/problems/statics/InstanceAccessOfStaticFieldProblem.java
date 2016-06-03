package com.jetbrains.pluginverifier.problems.statics;

import com.jetbrains.pluginverifier.problems.Problem;
import com.jetbrains.pluginverifier.utils.Pair;
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

  public InstanceAccessOfStaticFieldProblem(String field) {
    myField = field;
  }

  @NotNull
  @Override
  public String getDescriptionPrefix() {
    return "attempt to perform instance access on a static field";
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
    return getDescriptionPrefix() + " " + myField;
  }

  @Override
  public Problem deserialize(String... params) {
    return new InstanceAccessOfStaticFieldProblem(params[0]);
  }

  @Override
  public List<Pair<String, String>> serialize() {
    return Collections.singletonList(Pair.create("field", myField));
  }


}
