package com.jetbrains.pluginverifier.problems.statics;

import com.jetbrains.pluginverifier.problems.Problem;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by Sergey Patrikeev
 */
@XmlRootElement
public class StaticAccessOfInstanceFieldProblem extends Problem {

  private String myField;

  public StaticAccessOfInstanceFieldProblem() {
  }

  public StaticAccessOfInstanceFieldProblem(String field) {
    myField = field;
  }

  @NotNull
  @Override
  public String getDescriptionPrefix() {
    return "attempt to perform static access on an instance field";
  }

  @NotNull
  @Override
  public String getDescription() {
    return getDescriptionPrefix() + " " + myField;
  }

  public String getField() {
    return myField;
  }

  public void setField(String field) {
    myField = field;
  }

}
