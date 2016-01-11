package com.intellij.structure.domain;

import com.intellij.structure.errors.IncorrectPluginException;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

/**
 * @author Sergey Patrikeev
 */
public abstract class PluginManager {

  @NotNull
  public abstract Plugin createPlugin(@NotNull File pluginFile) throws IOException, IncorrectPluginException;

}
