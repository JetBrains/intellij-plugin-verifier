package com.jetbrains.pluginverifier;

import com.jetbrains.pluginverifier.util.Consumer;
import com.jetbrains.pluginverifier.problems.Problem;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Sergey Evdokimov
 */
public class ProblemsCollector implements Consumer<Problem> {

  private final List<Problem> myProblems = new ArrayList<Problem>();

  @Override
  public void consume(Problem problem) {
    myProblems.add(problem);
  }

  public List<Problem> getProblems() {
    return myProblems;
  }
}
