package com.intellij.structure.plugin;

import com.intellij.structure.impl.domain.PluginManagerImpl;
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
  final public PluginCreationResult createPlugin(@NotNull File pluginFile) throws IOException {
    return createPlugin(pluginFile, false);
  }

  @NotNull
  public abstract PluginCreationResult createPlugin(@NotNull File pluginFile, boolean readClassFiles) throws IOException;

}
