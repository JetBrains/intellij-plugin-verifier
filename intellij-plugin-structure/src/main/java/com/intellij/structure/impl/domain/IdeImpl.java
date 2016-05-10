package com.intellij.structure.impl.domain;

import com.intellij.structure.domain.Ide;
import com.intellij.structure.domain.IdeVersion;
import com.intellij.structure.domain.Plugin;
import com.intellij.structure.errors.IncorrectPluginException;
import com.intellij.structure.impl.utils.StringUtil;
import com.intellij.structure.resolvers.Resolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class IdeImpl implements Ide {
  private final Resolver myResolver;
  private final List<Plugin> myBundledPlugins;
  private final List<Plugin> myCustomPlugins;

  private final IdeVersion myVersion;

  IdeImpl(@NotNull IdeVersion version,
          @NotNull Resolver resolver,
          @NotNull List<Plugin> bundledPlugins) throws IOException, IncorrectPluginException {
    myVersion = version;
    myResolver = resolver;
    myBundledPlugins = bundledPlugins;
    myCustomPlugins = new ArrayList<Plugin>();
  }

  private IdeImpl(@NotNull Resolver resolver, @NotNull List<Plugin> bundledPlugins, @NotNull List<Plugin> customPlugins, @NotNull IdeVersion version) {
    myResolver = resolver;
    myBundledPlugins = bundledPlugins;
    myCustomPlugins = customPlugins;
    myVersion = version;
  }

  @NotNull
  @Override
  public IdeVersion getVersion() {
    return myVersion;
  }

  @NotNull
  @Override
  public Ide getExpandedIde(@NotNull Plugin plugin) {
    List<Plugin> newCustoms = new ArrayList<Plugin>(myCustomPlugins);
    newCustoms.add(plugin);
    return new IdeImpl(myResolver, myBundledPlugins, newCustoms, myVersion);
  }

  @Override
  @NotNull
  public List<Plugin> getCustomPlugins() {
    return Collections.unmodifiableList(myCustomPlugins);
  }

  @Override
  @NotNull
  public List<Plugin> getBundledPlugins() {
    return Collections.unmodifiableList(myBundledPlugins);
  }

  @Override
  @Nullable
  public Plugin getPluginById(@NotNull String pluginId) {
    for (Plugin plugin : myCustomPlugins) {
      String id = plugin.getPluginId() != null ? plugin.getPluginId() : plugin.getPluginName();
      if (StringUtil.equal(id, pluginId)) {
        return plugin;
      }
    }
    for (Plugin plugin : myBundledPlugins) {
      String id = plugin.getPluginId() != null ? plugin.getPluginId() : plugin.getPluginName();
      if (StringUtil.equal(id, pluginId))
        return plugin;
    }
    return null;
  }


  @NotNull
  @Override
  public Resolver getResolver() {
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
