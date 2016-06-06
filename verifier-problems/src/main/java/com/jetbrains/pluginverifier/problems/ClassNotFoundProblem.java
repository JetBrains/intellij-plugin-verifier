package com.jetbrains.pluginverifier.problems;

import com.google.common.base.Preconditions;
import com.jetbrains.pluginverifier.utils.MessageUtils;
import com.jetbrains.pluginverifier.utils.Pair;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Collections;
import java.util.List;

@XmlRootElement
public class ClassNotFoundProblem extends Problem {

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
  @Override
  public String getDescriptionPrefix() {
    return "accessing to unknown class";
  }

  @NotNull
  public String getDescription() {
    return getDescriptionPrefix() + " " + MessageUtils.convertClassName(myUnknownClass);
  }

  @Override
  public Problem deserialize(String... params) {
    return new ClassNotFoundProblem(params[0]);
  }

  @Override
  public List<Pair<String, String>> serialize() {
    return Collections.singletonList(Pair.create("unknownClass", myUnknownClass));
  }



}
