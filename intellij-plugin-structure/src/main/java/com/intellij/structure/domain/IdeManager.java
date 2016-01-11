package com.intellij.structure.domain;

import com.intellij.structure.errors.IncorrectPluginException;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

/**
 * @author Sergey Patrikeev
 */
public abstract class IdeManager {

  @NotNull
  public abstract Ide createIde(@NotNull File idePath) throws IOException, IncorrectPluginException;


}
