package com.intellij.structure.plugin;

import com.google.common.collect.Multimap;
import com.intellij.structure.ide.IdeVersion;
import com.jetbrains.structure.plugin.Plugin;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Sergey Patrikeev
 */
public interface IdePlugin extends Plugin {
  boolean isCompatibleWithIde(@NotNull IdeVersion ideVersion);

  @NotNull
  Multimap<String, Element> getExtensions();

  @NotNull
  List<PluginDependency> getDependencies();

  String getPluginName();

  String getPluginVersion();

  String getPluginId();

  String getVendor();

  @NotNull
  Set<String> getDefinedModules();

  @NotNull
  Set<String> getAllClassesReferencedFromXml();

  @NotNull
  Map<String, IdePlugin> getOptionalDescriptors();

  @NotNull
  Document getUnderlyingDocument();

  @Nullable
  File getOriginalFile();
}
