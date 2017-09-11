package com.jetbrains.plugin.structure.intellij.plugin;

import com.jetbrains.plugin.structure.base.plugin.PluginCreationResult;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

/**
 * @author Sergey Patrikeev
 */
public abstract class IdePluginManager {

  @NotNull
  public static IdePluginManager getInstance() {
    return new IdePluginManagerImpl();
  }

  @NotNull
  public abstract PluginCreationResult<IdePlugin> createPlugin(@NotNull File pluginFile) throws IOException;

}
