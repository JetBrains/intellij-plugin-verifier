/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide;

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin;
import com.jetbrains.plugin.structure.intellij.plugin.PluginProvider;
import com.jetbrains.plugin.structure.intellij.plugin.PluginProvision;
import com.jetbrains.plugin.structure.intellij.plugin.PluginQuery;
import com.jetbrains.plugin.structure.intellij.version.IdeVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import static com.jetbrains.plugin.structure.intellij.plugin.PluginProviderResult.Type.MODULE;
import static com.jetbrains.plugin.structure.intellij.plugin.PluginProviderResult.Type.PLUGIN;

/**
 * An IDE instance consisting of the class-files and plugins.
 * IDE can be created via {@link IdeManager#createIde(java.nio.file.Path)}.
 */
public abstract class Ide implements PluginProvider {
  private final PluginQueryMatcher queryMatcher = new PluginQueryMatcher();

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
   * Return a bundled plugin with a corresponding ID or with a corresponding alias (module ID).
   * The plugin resolution type is provided with a plugin instance.
   * @param pluginIdOrModuleId plugin ID or alias
   * @return bundled plugin with a resolution type.
   */
  @Override
  public @Nullable PluginProviderResult findPluginByIdOrModuleId(@NotNull String pluginIdOrModuleId) {
    for (IdePlugin plugin : getBundledPlugins()) {
      String id = getPluginId(plugin);
      if (Objects.equals(id, pluginIdOrModuleId)) {
        return new PluginProviderResult(PLUGIN, plugin);
      } else if (plugin.getDefinedModules().contains(pluginIdOrModuleId)) {
        return new PluginProviderResult(MODULE, plugin);
      }
    }
    return null;
  }

  /**
   * Finds bundled plugin according to specified query.
   *
   * @param query plugin search query.
   * @return a plugin provision class of corresponding type. Never returns {@code null}.
   */
  @Override
  public @NotNull PluginProvision query(@NotNull PluginQuery query) {
    for (IdePlugin plugin : getBundledPlugins()) {
      PluginProvision pluginProvision = queryMatcher.matches(plugin, query);
      if (pluginProvision instanceof PluginProvision.Found) {
        return pluginProvision;
      }
    }
    return PluginProvision.NotFound.INSTANCE;
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

  /**
   * Returns the plugin identifier. It is either a plugin proper ID or a plugin name, as a fallback.
   * @param plugin plugin to get the identifier for.
   * @return the plugin identifier.
   */
  @Nullable
  protected String getPluginId(IdePlugin plugin) {
    return plugin.getPluginId() != null ? plugin.getPluginId() : plugin.getPluginName();
  }
}
