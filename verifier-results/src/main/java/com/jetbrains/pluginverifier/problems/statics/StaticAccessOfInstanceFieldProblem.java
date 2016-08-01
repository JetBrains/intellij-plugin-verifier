package com.jetbrains.pluginverifier.problems.statics;

import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;
import com.jetbrains.pluginverifier.problems.Problem;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Sergey Patrikeev
 */

public class StaticAccessOfInstanceFieldProblem extends Problem {

  @SerializedName("field")
  private String myField;

  public StaticAccessOfInstanceFieldProblem() {
  }

  public StaticAccessOfInstanceFieldProblem(@NotNull String field) {
    Preconditions.checkNotNull(field);
    myField = field;
  }

  @NotNull
  @Override
  public String getDescription() {
    return "attempt to perform static access on an instance field" + " " + myField;
  }


  public String getField() {
    return myField;
  }

  public void setField(String field) {
    myField = field;
  }

}
