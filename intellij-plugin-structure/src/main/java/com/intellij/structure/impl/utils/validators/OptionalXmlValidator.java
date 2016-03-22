package com.intellij.structure.impl.utils.validators;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Sergey Patrikeev
 */
public class OptionalXmlValidator extends Validator {

  @Override
  public void onMissingFile(@NotNull String message) throws RuntimeException {
    onIncorrectStructure(message);
  }

  @Override
  public void onIncorrectStructure(@NotNull String message, @Nullable Throwable cause) {
    System.err.println(message);
    if (cause != null) {
      cause.printStackTrace();
    }
  }

  @Override
  public void onCheckedException(@NotNull String message, @NotNull Exception cause) {
    System.err.println(message);
    cause.printStackTrace();
  }

}
