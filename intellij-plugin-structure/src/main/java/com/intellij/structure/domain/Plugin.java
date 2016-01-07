package com.intellij.structure.domain;

import com.intellij.structure.pool.ClassPool;
import org.jdom.Document;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Sergey Patrikeev
 */
public interface Plugin {
  @NotNull
  Document getPluginXml();

  @NotNull
  Map<String, Document> getAllXmlInRoot();

  @Nullable
  IdeVersion getSinceBuild();

  @Nullable
  IdeVersion getUntilBuild();

  boolean isCompatibleWithIde(@NotNull String ideVersion);

  @NotNull
  List<PluginDependency> getDependencies();

  @NotNull
  List<PluginDependency> getModuleDependencies();

  @NotNull
  String getPluginName();

  @NotNull
  String getPluginVersion();

  @NotNull
  String getPluginId();

  @NotNull
  String getPluginVendor();

  @NotNull
  Set<String> getDefinedModules();

  @NotNull
  ClassPool getPluginClassPool();

  @NotNull
  ClassPool getLibraryClassPool();

  @NotNull
  ClassPool getAllClassesPool();
}
