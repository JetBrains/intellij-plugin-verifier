package com.jetbrains.plugin.structure.intellij.plugin;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface PluginProvider {
    /**
     * Finds bundled plugin with specified plugin ID.
     *
     * @param pluginId plugin ID
     * @return bundled plugin with the specified ID or {@code null}, if such a plugin is not found
     */
    @Nullable
    IdePlugin getPluginById(@NotNull String pluginId);

    /**
     * Finds bundled plugin containing the definition of the given module.
     *
     * @param moduleId module ID
     * @return bundled plugin with a definition of the module or {@code null}, if such a plugin is not found
     */
    @Nullable
    public IdePlugin getPluginByModule(@NotNull String moduleId);
}
