/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide;

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin;
import com.jetbrains.plugin.structure.intellij.plugin.PluginProvider;
import com.jetbrains.plugin.structure.intellij.version.IdeVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * An IDE instance consisting of the class-files and plugins.
 * IDE can be created via {@link IdeManager#createIde(java.nio.file.Path)}.
 */
public abstract class Ide implements PluginProvider {
  /**
   * Returns the IDE version either from 'build.txt' or specified with {@link IdeManager#createIde(java.nio.file.Path, IdeVersion)}
   *
   * @return ide version of {@code this} instance
   */
  @NotNull
  public abstract IdeVersion getVersion();

  /**
   * Returns the list of default plugins bundled with the IDE distribution to provide its work.
   *
   * @return the list of bundled plugins
   */
  @NotNull
  public abstract List<IdePlugin> getBundledPlugins();

  /**
   * Indicates if this IDE contains a specified plugin.
   *
   * @return {@code true} if such plugin is available in the IDE as a bundled plugin.
   */
  public boolean hasBundledPlugin(@NotNull String pluginId) {
    return findPluginById(pluginId) != null;
  }

  /**
   * Finds bundled plugin with specified plugin id.
   *
   * @param pluginId plugin id
   * @return bundled plugin with the specified id, or null if such plugin is not found
   */
  @Nullable
  @Override
  final public IdePlugin findPluginById(@NotNull String pluginId) {
    for (IdePlugin plugin : getBundledPlugins()) {
      String id = getId(plugin);
      if (Objects.equals(id, pluginId))
        return plugin;
    }
    return null;
  }

  /**
   * Finds bundled plugin containing the definition of the given module.
   *
   * @param moduleId module id
   * @return bundled plugin with definition of the module, or null if such plugin is not found
   */
  @Nullable
  @Override
  final public IdePlugin findPluginByModule(@NotNull String moduleId) {
    for (IdePlugin plugin : getBundledPlugins()) {
      if (plugin.getDefinedModules().contains(moduleId)) {
        return plugin;
      }
    }
    return null;
  }

  /**
   * Returns a plugin ID of the specified plugin. It uses the ID of the plugin if it is specified,
   * or a plugin name if the ID is not specified.
   *
   * @param plugin plugin to get the ID of
   * @return plugin identifier of the specified plugin
   */
  @Nullable
  protected String getId(@NotNull IdePlugin plugin) {
    String id = plugin.getPluginId();
    if (id == null) {
      id = plugin.getPluginName();
    }
    return id;
  }

  /**
   * Returns the file from which {@code this} Ide obtained.
   *
   * @return the path to the Ide instance
   */
  @NotNull
  public abstract Path getIdePath();
}
