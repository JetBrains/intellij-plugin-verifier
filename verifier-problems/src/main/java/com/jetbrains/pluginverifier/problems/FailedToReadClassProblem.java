package com.jetbrains.pluginverifier.problems;

import com.jetbrains.pluginverifier.utils.MessageUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class FailedToReadClassProblem extends Problem {

  public FailedToReadClassProblem() {

  }

  public FailedToReadClassProblem(@NotNull String className) {
    setLocation(new ProblemLocation(className));
  }

  @Override
  public String getDescription() {
    return "failed to read class: " + MessageUtils.convertClassName(getLocation().getClassName());
  }
}
