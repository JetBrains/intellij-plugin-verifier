package com.intellij.structure.impl.utils.validators;

import com.intellij.structure.errors.IncorrectPluginException;
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
    return new BiFunction<String, Throwable, Void>() {
      @Override
      public Void apply(String s, Throwable throwable) {
        //always throw an IncorrectPluginException
        throw new IncorrectPluginException(s, throwable);
      }
    };
  }

}
