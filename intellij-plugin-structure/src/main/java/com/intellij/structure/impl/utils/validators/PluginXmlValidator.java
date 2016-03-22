package com.intellij.structure.impl.utils.validators;

import com.intellij.structure.errors.IncorrectPluginException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Sergey Patrikeev
 */
public class PluginXmlValidator extends Validator {

  @Override
  public void onMissingFile(@NotNull String message) throws RuntimeException {
    throw new IncorrectPluginException(message);
  }

  @Override
  public void onIncorrectStructure(@NotNull String message, @Nullable Throwable cause) {
    throw new IncorrectPluginException(message, cause);
  }

  @Override
  public void onCheckedException(@NotNull String message, @NotNull Exception cause) {
    throw new IncorrectPluginException(message, cause);
  }
}
