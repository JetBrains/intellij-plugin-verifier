package com.intellij.structure.impl.domain;

import com.intellij.structure.domain.Ide;
import com.intellij.structure.domain.IdeVersion;
import com.intellij.structure.domain.Plugin;
import com.intellij.structure.errors.IncorrectPluginException;
import com.intellij.structure.impl.utils.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class IdeImpl implements Ide {
  private final List<Plugin> myBundledPlugins;
  private final List<Plugin> myCustomPlugins;

  private final IdeVersion myVersion;
  private final File myIdePath;

  IdeImpl(@NotNull File idePath,
          @NotNull IdeVersion version,
          @NotNull List<Plugin> bundledPlugins) throws IOException, IncorrectPluginException {
    this(idePath, bundledPlugins, Collections.<Plugin>emptyList(), version);
  }

  private IdeImpl(@NotNull File idePath, @NotNull List<Plugin> bundledPlugins, @NotNull List<Plugin> customPlugins, @NotNull IdeVersion version) {
    myIdePath = idePath;
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
    return new IdeImpl(myIdePath, myBundledPlugins, newCustoms, myVersion);
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

  @NotNull
  @Override
  public File getIdePath() {
    return myIdePath;
  }

  @Override
  public String toString() {
    return myVersion.asString();
  }
}
