package com.jetbrains.plugin.structure.intellij.plugin;

import com.google.common.collect.Multimap;
import com.jetbrains.plugin.structure.base.plugin.Plugin;
import com.jetbrains.plugin.structure.intellij.version.IdeVersion;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface IdePlugin extends Plugin {
  @Nullable
  IdeVersion getSinceBuild();

  @Nullable
  IdeVersion getUntilBuild();

  boolean isCompatibleWithIde(@NotNull IdeVersion ideVersion);

  @NotNull
  Multimap<String, Element> getExtensions();

  @NotNull
  List<PluginDependency> getDependencies();

  @NotNull
  Set<String> getDefinedModules();

  @NotNull
  Map<String, IdePlugin> getOptionalDescriptors();

  @NotNull
  Document getUnderlyingDocument();

  @Nullable
  File getOriginalFile();

  @Nullable
  ProductDescriptor getProductDescriptor();
}
