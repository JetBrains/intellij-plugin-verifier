package com.intellij.structure.impl.domain;

import com.intellij.structure.domain.Ide;
import com.intellij.structure.domain.IdeVersion;
import com.intellij.structure.domain.Plugin;
import com.intellij.structure.errors.IncorrectPluginException;
import com.intellij.structure.resolvers.Resolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class Idea implements Ide {
  private final Resolver myResolver;
  private final List<Plugin> myBundledPlugins;
  private final List<Plugin> myCustomPlugins;

  private IdeVersion myVersion;

  Idea(@Nullable IdeVersion version,
       @NotNull Resolver resolver,
       @NotNull List<Plugin> bundledPlugins) throws IOException, IncorrectPluginException {
    myVersion = version;
    myResolver = resolver;
    myBundledPlugins = bundledPlugins;
    myCustomPlugins = new ArrayList<Plugin>();
  }

  @NotNull
  @Override
  public IdeVersion getVersion() {
    return myVersion;
  }

  @Override
  public void updateVersion(@NotNull IdeVersion newVersion) {
    myVersion = newVersion;
  }

  @Override
  public void addCustomPlugin(@NotNull Plugin plugin) {
    myCustomPlugins.add(plugin);
  }

  @Override
  @NotNull
  public List<Plugin> getCustomPlugins() {
    return myCustomPlugins;
  }

  @Override
  @NotNull
  public List<Plugin> getBundledPlugins() {
    return myBundledPlugins;
  }

  @Override
  @Nullable
  public Plugin getPluginById(@NotNull String pluginId) {
    for (Plugin plugin : myCustomPlugins) {
      if (plugin.getPluginId().equals(pluginId)) {
        return plugin;
      }
    }
    for (Plugin plugin : myBundledPlugins) {
      if (plugin.getPluginId().equals(pluginId))
        return plugin;
    }
    return null;
  }


  @NotNull
  @Override
  public Resolver getClassPool() {
    return myResolver;
  }

  @Override
  @Nullable
  public Plugin getPluginByModule(@NotNull String moduleId) {
    for (Plugin plugin : myCustomPlugins) {
      if (plugin.getDefinedModules().contains(moduleId)) {
        return plugin;
      }
    }
    for (Plugin plugin : myBundledPlugins) {
      if (plugin.getDefinedModules().contains(moduleId)) {
        return plugin;
      }
    }
    return null;
  }
}
