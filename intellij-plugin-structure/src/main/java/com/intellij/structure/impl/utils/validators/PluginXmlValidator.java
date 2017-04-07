package com.intellij.structure.impl.utils.validators;

import com.intellij.structure.domain.PluginProblem;
import com.intellij.structure.impl.domain.PluginProblemImpl;
import com.intellij.structure.impl.utils.BiAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Sergey Patrikeev
 */
public class PluginXmlValidator extends Validator {

  private final BiAction<String, Throwable> errorReportingAction = new BiAction<String, Throwable>() {
    @Override
    public void call(String s, Throwable throwable) {
      myProblems.add(new PluginProblemImpl(s, PluginProblem.Level.ERROR));
    }
  };

  private final BiAction<String, Throwable> warningReportingAction = new BiAction<String, Throwable>() {
    @Override
    public void call(String s, Throwable throwable) {
      myProblems.add(new PluginProblemImpl(s, PluginProblem.Level.WARNING));
    }
  };

  @Nullable
  @Override
  protected BiAction<String, Throwable> supplyAction(@NotNull Event event) {
    switch (event) {
      case MULTIPLE_CONFIG_FILE:
      case MISSING_DEPENDENCY:
        return warningReportingAction;
      default:
        return errorReportingAction;
    }
  }
}
