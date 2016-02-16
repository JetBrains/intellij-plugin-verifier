package com.intellij.structure.domain;

import com.intellij.structure.resolvers.Resolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author Sergey Patrikeev
 */
public interface Ide {
  @NotNull
  IdeVersion getVersion();

  /**
   * Returns an immutable copy of {@code this} IDE with a specified plugin added to the list of custom plugins. It
   * allows us to refer the plugin by its defined modules by invoking {@link #getPluginByModule}. <p>Of course,
   * invocation of {@link #getCustomPlugins()} on the result will contain a specified plugin
   *
   * @param plugin plugin to be added to the custom plugins
   * @return copy of this Ide with added {@code plugin}
   */
  @NotNull
  Ide expandedIde(@NotNull Plugin plugin);

  @NotNull
  List<Plugin> getCustomPlugins();

  @NotNull
  List<Plugin> getBundledPlugins();

  @Nullable
  Plugin getPluginById(@NotNull String pluginId);

  @Nullable
  Plugin getPluginByModule(@NotNull String moduleId);

  @NotNull
  Resolver getClassPool();
}
