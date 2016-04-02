package com.intellij.structure.impl.utils.validators;

import com.intellij.structure.impl.utils.BiFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Sergey Patrikeev
 */
public abstract class Validator {

  private void performAction(@NotNull Event event, @NotNull String message, @Nullable Throwable cause) {
    BiFunction<String, Throwable, Void> function = supplyAction(event);
    if (function != null) {
      function.apply(message, cause);
    }
  }

  public void onMissingConfigElement(@NotNull String message) throws RuntimeException {
    performAction(Event.MISSING_CONFIG_ELEMENT, message, null);
  }

  public void onMissingFile(@NotNull String message) throws RuntimeException {
    performAction(Event.MISSING_FILE, message, null);
  }

  public void onIncorrectStructure(@NotNull String message) throws RuntimeException {
    performAction(Event.INCORRECT_STRUCTURE, message, null);
  }

  public void onCheckedException(@NotNull String message, @NotNull Exception cause) throws RuntimeException {
    performAction(Event.CHECKED_EXCEPTION, message, cause);
  }

  @Nullable
  protected abstract BiFunction<String, Throwable, Void> supplyAction(@NotNull Event event);

  private Validator createIgnoringValidator(@NotNull final Event ignoredEvent) {
    return new Validator() {
      @Nullable
      @Override
      protected BiFunction<String, Throwable, Void> supplyAction(@NotNull Event event) {
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


  enum Event {
    MISSING_FILE,
    INCORRECT_STRUCTURE,
    CHECKED_EXCEPTION,
    MISSING_CONFIG_ELEMENT
  }

}
