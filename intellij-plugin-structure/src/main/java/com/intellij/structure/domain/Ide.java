package com.intellij.structure.domain;

import com.intellij.structure.pool.ClassPool;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author Sergey Patrikeev
 */
public interface Ide {
  @NotNull
  String getVersion();

  void updateVersion(@NotNull String newVersion);

  void addCustomPlugin(@NotNull Plugin plugin);

  @NotNull
  List<Plugin> getCustomPlugins();

  @NotNull
  List<Plugin> getBundledPlugins();

  @Nullable
  Plugin getPluginById(@NotNull String pluginId);

  @Nullable
  Plugin getPluginByModule(@NotNull String moduleId);

  @NotNull
  ClassPool getClassPool();
}
