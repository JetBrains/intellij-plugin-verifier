package com.jetbrains.pluginverifier.problems;

import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;
import com.jetbrains.pluginverifier.utils.MessageUtils;
import org.jetbrains.annotations.NotNull;


public class ClassNotFoundProblem extends Problem {

  @SerializedName("class")
  private String myUnknownClass;

  public ClassNotFoundProblem() {

  }

  public ClassNotFoundProblem(String unknownClass) {
    Preconditions.checkNotNull(unknownClass);
    myUnknownClass = unknownClass;
  }

  public String getUnknownClass() {
    return myUnknownClass;
  }

  public void setUnknownClass(String unknownClass) {
    myUnknownClass = unknownClass;
  }

  @NotNull
  public String getDescription() {
    return "accessing to unknown class" + " " + MessageUtils.INSTANCE.convertClassName(myUnknownClass);
  }

}
