package com.jetbrains.pluginverifier.problems;

import com.google.common.base.Preconditions;
import com.jetbrains.pluginverifier.utils.MessageUtils;
import com.jetbrains.pluginverifier.utils.Pair;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Collections;
import java.util.List;

@XmlRootElement
public class InheritFromFinalClassProblem extends Problem {

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
    return "cannot inherit from final class " + MessageUtils.convertClassName(myFinalClass);
  }

  @Override
  public Problem deserialize(String... params) {
    return new InheritFromFinalClassProblem(params[0]);
  }

  @Override
  public List<Pair<String, String>> serialize() {
    return Collections.singletonList(Pair.create("finalClass", myFinalClass));
  }


}
