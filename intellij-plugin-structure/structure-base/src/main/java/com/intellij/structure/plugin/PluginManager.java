package com.intellij.structure.plugin;

import com.intellij.structure.impl.domain.PluginManagerImpl;
import com.jetbrains.structure.plugin.PluginCreationResult;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

/**
 * @author Sergey Patrikeev
 */
public abstract class PluginManager {

  @NotNull
  public static PluginManager getInstance() {
    return new PluginManagerImpl();
  }

  @NotNull
  public abstract PluginCreationResult<IdePlugin> createPlugin(@NotNull File pluginFile) throws IOException;

}
