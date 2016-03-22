package com.intellij.structure.impl.utils.validators;

import com.intellij.structure.impl.utils.BiFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Sergey Patrikeev
 */
public class OptionalXmlValidator extends Validator {

  @Nullable
  @Override
  protected BiFunction<String, Throwable, Void> supplyAction(@NotNull Event event) {
    return new BiFunction<String, Throwable, Void>() {
      @Override
      public Void apply(String s, Throwable throwable) {
        System.err.println(s);
        if (throwable != null) {
          throwable.printStackTrace();
        }
        return null;
      }
    };
  }

}
