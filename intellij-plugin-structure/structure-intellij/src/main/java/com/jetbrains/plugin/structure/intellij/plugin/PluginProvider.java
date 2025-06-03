/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface PluginProvider {
    /**
     * Finds a plugin with specified plugin ID.
     *
     * @param pluginId plugin ID
     * @return plugin with the specified ID or {@code null}, if such a plugin is not found
     */
    @Nullable
    IdePlugin findPluginById(@NotNull String pluginId);

    /**
     * Finds a plugin containing the definition of the given module.
     *
     * @param moduleId module ID
     * @return plugin with a definition of the module or {@code null}, if such a plugin is not found
     */
    @Nullable
    IdePlugin findPluginByModule(@NotNull String moduleId);

    /**
     * Finds a plugin with a specified ID or a plugin containing the definition of the given module.
     * This plugin is wrapped with a result object indicating the resolution type (either a plugin or a module).
     * @param pluginIdOrModuleId plugin ID or module ID that is exposed by the plugin.
     * @return plugin or a plugin with a definition of the module or {@code null}, if such a plugin is not found
     */
    @Nullable
    default PluginProviderResult findPluginByIdOrModuleId(@NotNull String pluginIdOrModuleId) {
        IdePlugin plugin = findPluginById(pluginIdOrModuleId);
        if (plugin != null) {
            return new PluginProviderResult(PLUGIN, plugin);
        } else {
            plugin = findPluginByModule(pluginIdOrModuleId);
            if (plugin != null) {
                return new PluginProviderResult(MODULE, plugin);
            } else {
                return null;
            }
        }
    }

    @NotNull
    PluginProvision query(@NotNull PluginQuery query);
}
