package com.intellij.structure.domain;

import com.intellij.structure.errors.BrokenPluginException;
import com.intellij.structure.impl.domain.IdeaPluginManager;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

/**
 * @author Sergey Patrikeev
 */
public abstract class PluginManager {

  private static PluginManager ourInstance;

  public static PluginManager getInstance() {
    if (ourInstance == null) {
      //change if necessary
      ourInstance = new IdeaPluginManager();
    }
    return ourInstance;
  }

  @NotNull
  public abstract Plugin createPlugin(@NotNull File pluginFile) throws IOException, BrokenPluginException;

  @NotNull
  public Plugin createPlugin(@NotNull String pluginPath) throws IOException, BrokenPluginException {
    return createPlugin(new File(pluginPath));
  }
}
