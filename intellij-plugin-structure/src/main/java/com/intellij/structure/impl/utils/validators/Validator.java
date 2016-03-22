package com.intellij.structure.impl.utils.validators;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Sergey Patrikeev
 */
public abstract class Validator {

  public void onIncorrectStructure(@NotNull String message) throws RuntimeException {
    onIncorrectStructure(message, null);
  }

  public abstract void onMissingFile(@NotNull String message) throws RuntimeException;

  public abstract void onIncorrectStructure(@NotNull String message, @Nullable Throwable cause) throws RuntimeException;

  public abstract void onCheckedException(@NotNull String message, @NotNull Exception cause) throws RuntimeException;

  public Validator getMissingFileIgnoringValidator() {
    return new Validator() {
      @Override
      public void onMissingFile(@NotNull String message) throws RuntimeException {
        //do nothing
      }

      @Override
      public void onIncorrectStructure(@NotNull String message, @Nullable Throwable cause) throws RuntimeException {
        Validator.this.onIncorrectStructure(message, cause);
      }

      @Override
      public void onCheckedException(@NotNull String message, @NotNull Exception cause) throws RuntimeException {
        Validator.this.onCheckedException(message, cause);
      }
    };
  }

}
