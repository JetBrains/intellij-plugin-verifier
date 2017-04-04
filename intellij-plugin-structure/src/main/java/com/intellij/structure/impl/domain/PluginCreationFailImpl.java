package com.intellij.structure.impl.domain;

import com.intellij.structure.domain.PluginCreationFail;
import com.intellij.structure.domain.PluginCreationResult;
import com.intellij.structure.domain.PluginProblem;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PluginCreationFailImpl implements PluginCreationFail {
  @NotNull private final List<PluginProblem> myProblems;

  public PluginCreationFailImpl(@NotNull List<PluginProblem> problems) {
    myProblems = problems;
  }

  @Override
  @NotNull
  public List<PluginProblem> getErrorsAndWarnings() {
    return myProblems;
  }
}
