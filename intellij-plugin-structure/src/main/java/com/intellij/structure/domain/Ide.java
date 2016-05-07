package com.intellij.structure.domain;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;

/**
 * An IDE instance consisting of the class-files and plugins.
 * IDE can be created via {@link IdeManager#createIde(File)}.
 */
public interface Ide {
  /**
   * Returns the IDE version either from 'build.txt' or specified with {@link IdeManager#createIde(File, IdeVersion)}
   *
   * @return ide version of {@code this} instance
   */
  @NotNull
  IdeVersion getVersion();

  /**
   * Returns an immutable copy of {@code this} IDE with a specified plugin added to the list of custom plugins.
   * It allows us to refer the plugin by its defined modules by invoking {@link #getPluginByModule}
   * or with plugin id itself via {@link #getPluginById(String)}
   *
   * @param plugin plugin to be added to the custom plugins
   * @return copy of this Ide with added {@code plugin}
   */
  @NotNull
  Ide getExpandedIde(@NotNull Plugin plugin);

  /**
   * Returns the list of non-default plugins installed by the user. To emulate an installation of the plugin invoke
   * {@link #getExpandedIde(Plugin)}.
   *
   * @return the list of manually installed plugins
   */
  @NotNull
  List<Plugin> getCustomPlugins();

  /**
   * Returns the list of default plugins bundled with the IDE distribution to provide its work.
   *
   * @return the list of bundled plugins
   */
  @NotNull
  List<Plugin> getBundledPlugins();

  /**
   * Returns the plugin instance with the specified plugin id (it's a value of plugin.xml {@literal <id>} tag). The
   * plugin is either <i>custom</i> or <i>bundled</i>.
   *
   * @param pluginId plugin id
   * @return the plugin with the specified id
   */
  @Nullable
  Plugin getPluginById(@NotNull String pluginId);

  /**
   * Returns the plugin which has the definition of the given module.
   *
   * @param moduleId module id
   * @return the plugin with definition of the module
   */
  @Nullable
  Plugin getPluginByModule(@NotNull String moduleId);

  /**
   * Returns the file from which {@code this} Ide obtained.
   *
   * @return the path to the Ide instance
   */
  @NotNull
  File getIdePath();

}
