/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide;

import com.jetbrains.plugin.structure.intellij.plugin.*;
import com.jetbrains.plugin.structure.intellij.version.IdeVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jetbrains.plugin.structure.intellij.plugin.PluginProviderResult.Type.MODULE;
import static com.jetbrains.plugin.structure.intellij.plugin.PluginProviderResult.Type.PLUGIN;

/**
 * An IDE instance consisting of the class-files and plugins.
 * IDE can be created via {@link IdeManager#createIde(java.nio.file.Path)}.
 */
public abstract class Ide implements PluginProvider {
  private final PluginQueryMatcher queryMatcher = new PluginQueryMatcher();
  private volatile Map<String, IdePlugin> bundledPluginsById;
  private volatile Map<String, IdePlugin> bundledPluginsByModuleId;

  /**
   * Returns the IDE version either from 'build.txt' or specified with {@link IdeManager#createIde(java.nio.file.Path, IdeVersion)}
   *
   * @return ide version of {@code this} instance
   */
  @NotNull
  public abstract IdeVersion getVersion();

  /**
   * Returns the bundled plugins and IDE modules shipped with this IDE distribution.
   * <p>
   * For IDEs based on {@code product-info.json}, the list contains the core plugin ({@code com.intellij})
   * and the layout components of these kinds:
   * <ul>
   *   <li>{@code plugin}, such as {@code intellij.platform.ijent.impl}, loaded as a regular plugin.</li>
   *   <li>{@code productModuleV2} and {@code moduleV2}, such as {@code intellij.platform.diagnostic.freezeAnalyzer},
   *   loaded as IDE modules ({@link com.jetbrains.plugin.structure.intellij.plugin.module.IdeModule}).
   *   The plugin ID of an IDE module is its module name.</li>
   * </ul>
   *
   * @return the bundled plugins and IDE modules
   */
  @NotNull
  public abstract List<IdePlugin> getBundledPlugins();

  /**
   * Checks whether this IDE contains a bundled plugin or an IDE module with the given plugin ID.
   * <p>
   * By default, this is the same as {@code findPluginById(pluginId) != null}.
   * An IDE module ({@link com.jetbrains.plugin.structure.intellij.plugin.module.IdeModule})
   * is matched by its module name, such as {@code intellij.platform.diagnostic.freezeAnalyzer}.
   * Plugin aliases are not matched; use {@link #findPluginByModule(String)} for them.
   * <p>
   * Subclasses may override this with a cheaper check that does not load the bundled plugins,
   * see {@link ProductInfoBasedIde#hasBundledPlugin(String)}.
   *
   * @param pluginId plugin ID or IDE module name
   * @return {@code true} if a bundled plugin or an IDE module with this ID exists, {@code false} otherwise
   */
  public boolean hasBundledPlugin(@NotNull String pluginId) {
    return findPluginById(pluginId) != null;
  }

  /**
   * Finds a bundled plugin or an IDE module by its plugin ID.
   * <p>
   * An IDE module ({@link com.jetbrains.plugin.structure.intellij.plugin.module.IdeModule})
   * is matched by its module name, such as {@code intellij.platform.diagnostic.freezeAnalyzer}.
   * Plugin aliases are not matched; use {@link #findPluginByModule(String)} for them.
   *
   * @param pluginId plugin ID or IDE module name
   * @return the matching plugin or IDE module, or {@code null} if nothing matches
   */
  @Nullable
  @Override
  final public IdePlugin findPluginById(@NotNull String pluginId) {
    return getBundledPluginsById().get(pluginId);
  }

  /**
   * Finds a bundled plugin or an IDE module that defines the given module ID.
   * <p>
   * The module ID is matched against plugin aliases declared with {@code <module value="..."/>},
   * such as {@code com.intellij.modules.platform}, and against IDE module names, because an
   * {@link com.jetbrains.plugin.structure.intellij.plugin.module.IdeModule} defines its own name as a module.
   *
   * @param moduleId plugin alias or IDE module name
   * @return the plugin or IDE module that defines the module, or {@code null} if nothing matches
   */
  @Nullable
  @Override
  final public IdePlugin findPluginByModule(@NotNull String moduleId) {
    return getBundledPluginsByModuleId().get(moduleId);
  }

  /**
   * Finds a bundled plugin or an IDE module by a plugin ID, an IDE module name, or a plugin alias.
   * <p>
   * The lookup searches {@link #getBundledPlugins()} in this order, and the first match wins:
   * <ol>
   *   <li>By plugin ID, such as {@code intellij.platform.ijent.impl}. An IDE module
   *   ({@link com.jetbrains.plugin.structure.intellij.plugin.module.IdeModule}) is matched by its module name,
   *   such as {@code intellij.platform.diagnostic.freezeAnalyzer}.
   *   The result type is {@link PluginProviderResult.Type#PLUGIN}.</li>
   *   <li>By plugin alias declared with {@code <module value="..."/>},
   *   such as {@code com.intellij.modules.platform} declared by the {@code com.intellij} plugin.
   *   The result type is {@link PluginProviderResult.Type#MODULE}.</li>
   * </ol>
   *
   * @param pluginIdOrModuleId plugin ID, IDE module name, or plugin alias
   * @return the matching plugin or IDE module with its match type, or {@code null} if nothing matches
   */
  @Override
  public @Nullable PluginProviderResult findPluginByIdOrModuleId(@NotNull String pluginIdOrModuleId) {
    IdePlugin plugin = findPluginById(pluginIdOrModuleId);
    if (plugin != null) {
      return new PluginProviderResult(PLUGIN, plugin);
    }
    IdePlugin pluginByModule = getBundledPluginsByModuleId().get(pluginIdOrModuleId);
    if (pluginByModule != null) {
      return new PluginProviderResult(MODULE, pluginByModule);
    }
    return null;
  }

  /**
   * Finds the first bundled plugin or IDE module that matches any criterion enabled in the query.
   * <p>
   * The lookup scans {@link #getBundledPlugins()} in list order and returns the first plugin that matches.
   * Each plugin is checked against the enabled criteria in this order, and the first matching criterion
   * becomes the {@link PluginProvision.Source} of the result:
   * <ol>
   *   <li>{@link PluginQuery.Builder#inId()}: the plugin ID equals the identifier.
   *   The source is {@link PluginProvision.Source#ID}.</li>
   *   <li>{@link PluginQuery.Builder#inName()}: the plugin name equals the identifier.
   *   The source is {@link PluginProvision.Source#NAME}.</li>
   *   <li>{@link PluginQuery.Builder#inContentModuleId()}: the plugin declares a content module with this name.
   *   The source is {@link PluginProvision.Source#CONTENT_MODULE_ID}.</li>
   *   <li>{@link PluginQuery.Builder#inPluginAliases()}: the plugin defines a module with this ID,
   *   see {@link IdePlugin#hasDefinedModuleWithId(String)}.
   *   The source is {@link PluginProvision.Source#ALIAS}.</li>
   * </ol>
   * The plugin order takes precedence over the criterion order: a plugin earlier in the list that matches
   * by a later criterion wins over a plugin later in the list that matches by an earlier criterion.
   * This can differ from {@link #findPluginByIdOrModuleId(String)}, which checks all plugin IDs before any alias.
   * <p>
   * An IDE module ({@link com.jetbrains.plugin.structure.intellij.plugin.module.IdeModule}) is matched by its
   * module name, such as {@code intellij.platform.diagnostic.freezeAnalyzer}, through
   * {@link PluginQuery.Builder#inId()} and {@link PluginQuery.Builder#inPluginAliases()}.
   * It is never matched through {@link PluginQuery.Builder#inName()}, because it has no plugin name,
   * nor through {@link PluginQuery.Builder#inContentModuleId()}, because it declares no content modules.
   * <p>
   * Unlike the other lookups, this method does not use cached indexes, so each call scans all bundled plugins.
   *
   * @param query the identifier and the criteria to match it against
   * @return {@link PluginProvision.Found} with the matching plugin or IDE module and the matched criterion,
   * or {@link PluginProvision.NotFound} if nothing matches; never {@code null}
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

  private @NotNull Map<String, IdePlugin> getBundledPluginsById() {
    Map<String, IdePlugin> result = bundledPluginsById;
    if (result != null) {
      return result;
    }
    synchronized (this) {
      result = bundledPluginsById;
      if (result == null) {
        List<IdePlugin> bundledPlugins = getBundledPlugins();
        result = new HashMap<>(bundledPlugins.size());
        for (IdePlugin plugin : bundledPlugins) {
          String id = getId(plugin);
          if (id != null) {
            result.putIfAbsent(id, plugin);
          }
        }
        bundledPluginsById = result;
      }
      return result;
    }
  }

  @SuppressWarnings("deprecation")
  private @NotNull Map<String, IdePlugin> getBundledPluginsByModuleId() {
    Map<String, IdePlugin> result = bundledPluginsByModuleId;
    if (result != null) {
      return result;
    }
    synchronized (this) {
      result = bundledPluginsByModuleId;
      if (result == null) {
        result = new HashMap<>();
        for (IdePlugin plugin : getBundledPlugins()) {
          for (String moduleId : plugin.getDefinedModules()) {
            result.putIfAbsent(moduleId, plugin);
          }
        }
        bundledPluginsByModuleId = result;
      }
      return result;
    }
  }

  @Override
  public String getPresentableName() {
    return getVersion().asString();
  }
}
