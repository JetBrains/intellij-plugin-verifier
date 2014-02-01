package com.jetbrains.pluginverifier.utils;

import com.jetbrains.pluginverifier.problems.Problem;
import org.jetbrains.annotations.NotNull;

public class ToStringProblemComparator extends ToStringCachedComparator<Problem> {
  @NotNull
  @Override
  protected String toString(Problem object) {
    return object.getDescription();
  }
}
