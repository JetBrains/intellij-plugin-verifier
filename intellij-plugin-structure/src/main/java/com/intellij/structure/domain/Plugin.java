package com.intellij.structure.domain;

import com.intellij.structure.resolvers.Resolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
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

  @NotNull
  String getPluginName();

  @Nullable
  String getPluginVersion();

  @NotNull
  String getPluginId();

  @NotNull
  String getVendor();

  @NotNull
  Set<String> getDefinedModules();

  @NotNull
  Resolver getPluginResolver();

  @NotNull
  Resolver getLibraryResolver();

  @Nullable
  String getDescription();

  String getVendorEmail();

  String getVendorUrl();

//  String getVendorLogoPath(); //TODO: change it with InputStream

  String getUrl();

  String getChangeNotes();

  File getPluginPath();

}
