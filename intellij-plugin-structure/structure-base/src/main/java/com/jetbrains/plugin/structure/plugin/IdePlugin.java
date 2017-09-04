package com.jetbrains.plugin.structure.plugin;

import com.google.common.collect.Multimap;
import com.jetbrains.plugin.structure.ide.IdeVersion;
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
  @Nullable
  @Override
  IdeVersion getSinceBuild();

  @Nullable
  @Override
  IdeVersion getUntilBuild();

  boolean isCompatibleWithIde(@NotNull IdeVersion ideVersion);

  @NotNull
  Multimap<String, Element> getExtensions();

  @NotNull
  List<PluginDependency> getDependencies();

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
