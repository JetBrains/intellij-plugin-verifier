package com.intellij.structure.domain;

import com.intellij.structure.errors.IncorrectPluginException;
import com.intellij.structure.impl.domain.IdePluginManagerImpl;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

/**
 * @author Sergey Patrikeev
 */
public abstract class PluginManager {

  @NotNull
  public static PluginManager getPluginManager() {
    return new IdePluginManagerImpl();
  }

  @NotNull
  public abstract Plugin createPlugin(@NotNull File pluginFile) throws IOException, IncorrectPluginException;

}
