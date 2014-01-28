package com.jetbrains.pluginverifier.problems;

import com.jetbrains.pluginverifier.utils.MessageUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class FailedToReadClassProblem extends Problem {

  public static final FailedToReadClassProblem INSTANCE = new FailedToReadClassProblem();

  public FailedToReadClassProblem() {

  }

  @Override
  public String getDescription() {
    return "failed to read class";
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof FailedToReadClassProblem;
  }

  @Override
  public int hashCode() {
    return 24111985;
  }
}
