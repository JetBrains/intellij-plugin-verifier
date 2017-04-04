package com.intellij.structure.impl.utils.validators;

import com.intellij.structure.domain.PluginProblem;
import com.intellij.structure.impl.domain.PluginProblemImpl;
import com.intellij.structure.impl.utils.BiFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Sergey Patrikeev
 */
public class PluginXmlValidator extends Validator {
  @Nullable
  @Override
  protected BiFunction<String, Throwable, Void> supplyAction(@NotNull Event event) {
    BiFunction<String, Throwable, Void> errorReportingFunction = new BiFunction<String, Throwable, Void>() {
      @Override
      public Void apply(String s, Throwable throwable) {
        myProblems.add(new PluginProblemImpl(s, PluginProblem.Level.ERROR));
        return null;
      }
    };

    BiFunction<String, Throwable, Void> warningReportingFunction = new BiFunction<String, Throwable, Void>() {
      @Override
      public Void apply(String s, Throwable throwable) {
        myProblems.add(new PluginProblemImpl(s, PluginProblem.Level.WARNING));
        return null;
      }
    };

    switch (event) {
      case MULTIPLE_CONFIG_FILE:
      case MISSING_DEPENDENCY:
      case MISSING_LOGO:
        return warningReportingFunction;
      default:
        return errorReportingFunction;
    }
  }

}
