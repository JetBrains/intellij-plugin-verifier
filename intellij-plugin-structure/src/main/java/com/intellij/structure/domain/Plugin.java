package com.intellij.structure.domain;

import com.intellij.structure.resolvers.Resolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * @author Sergey Patrikeev
 */
public interface Plugin {

  @Nullable
  IdeVersion getSinceBuild();

  @Nullable
  IdeVersion getUntilBuild();

  boolean isCompatibleWithIde(@NotNull IdeVersion ideVersion);

  @NotNull
  List<PluginDependency> getDependencies();

  @NotNull
  List<PluginDependency> getModuleDependencies();

  String getPluginName();

  @Nullable
  String getPluginVersion();

  String getPluginId();

  String getVendor();

  @NotNull
  Set<String> getDefinedModules();

  @NotNull
  Resolver getPluginResolver();

  @Nullable
  String getDescription();

  @Nullable
  String getVendorEmail();

  @Nullable
  String getVendorUrl();

  @Nullable
  String getUrl();

  @Nullable
  String getChangeNotes();

  @NotNull
  Set<String> getAllClassesReferencedFromXml();

}
