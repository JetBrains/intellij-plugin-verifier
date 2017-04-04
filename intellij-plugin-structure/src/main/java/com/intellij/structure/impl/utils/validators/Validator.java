package com.intellij.structure.impl.utils.validators;

import com.intellij.structure.domain.PluginProblem;
import com.intellij.structure.impl.utils.BiAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Sergey Patrikeev
 */
public abstract class Validator {
  protected List<PluginProblem> myProblems = new ArrayList<PluginProblem>();

  private void performAction(@NotNull Event event, @NotNull String message, @Nullable Throwable cause) {
    BiAction<String, Throwable> action = supplyAction(event);
    if (action != null) {
      action.call(message, cause);
    }
  }

  public void onMissingConfigElement(@NotNull String message) throws RuntimeException {
    performAction(Event.MISSING_CONFIG_ELEMENT, message, null);
  }

  public void onMissingFile(@NotNull String message) throws RuntimeException {
    performAction(Event.MISSING_FILE, message, null);
  }

  public void onMissingLogo(@NotNull String message) throws RuntimeException {
    performAction(Event.MISSING_LOGO, message, null);
  }

  public void onMissingDependency(@NotNull String message) throws RuntimeException{
    performAction(Event.MISSING_DEPENDENCY, message, null);
  }

  public void onIncorrectStructure(@NotNull String message) throws RuntimeException {
    performAction(Event.INCORRECT_STRUCTURE, message, null);
  }

  public void onMultipleConfigFiles(@NotNull String message) throws RuntimeException {
    performAction(Event.MULTIPLE_CONFIG_FILE, message, null);
  }

  public void onCheckedException(@NotNull String message, @NotNull Exception cause) throws RuntimeException {
    performAction(Event.CHECKED_EXCEPTION, message, cause);
  }

  @Nullable
  protected abstract BiAction<String, Throwable> supplyAction(@NotNull Event event);

  private Validator createIgnoringValidator(@NotNull final Event ignoredEvent) {
    return new Validator() {
      @Nullable
      @Override
      protected BiAction<String, Throwable> supplyAction(@NotNull Event event) {
        if (event == ignoredEvent) {
          return null;
        }
        return Validator.this.supplyAction(event);
      }
    };
  }

  public Validator ignoreMissingFile() {
    return createIgnoringValidator(Event.MISSING_FILE);
  }

  public Validator ignoreMissingConfigElement() {
    return createIgnoringValidator(Event.MISSING_CONFIG_ELEMENT);
  }

  public List<PluginProblem> getProblems() {
    return myProblems;
  }


  enum Event {
    MISSING_FILE,
    INCORRECT_STRUCTURE,
    CHECKED_EXCEPTION,
    MISSING_CONFIG_ELEMENT,
    MISSING_DEPENDENCY,
    MULTIPLE_CONFIG_FILE,
    MISSING_LOGO
  }

}
