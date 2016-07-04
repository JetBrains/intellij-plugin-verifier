package com.jetbrains.pluginverifier.problems;

import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;
import com.jetbrains.pluginverifier.utils.MessageUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class InheritFromFinalClassProblem extends Problem {

  @SerializedName("finalClass")
  private String myFinalClass;

  public InheritFromFinalClassProblem() {

  }

  public InheritFromFinalClassProblem(@NotNull String finalClass) {
    Preconditions.checkNotNull(finalClass);
    myFinalClass = finalClass;
  }

  public String getFinalClass() {
    return myFinalClass;
  }

  public void setFinalClass(String finalClass) {
    myFinalClass = finalClass;
  }

  @NotNull
  public String getDescription() {
    return "cannot inherit from final class " + MessageUtils.INSTANCE.convertClassName(myFinalClass);
  }

}
