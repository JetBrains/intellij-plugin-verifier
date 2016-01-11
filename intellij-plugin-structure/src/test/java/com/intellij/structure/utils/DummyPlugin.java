package com.intellij.structure.utils;

import com.intellij.structure.domain.IdeVersion;
import com.intellij.structure.domain.Plugin;
import com.intellij.structure.domain.PluginDependency;
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
public class DummyPlugin implements Plugin {
  @NotNull
  @Override
  public Document getPluginXml() {
    return null;
  }

  @NotNull
  @Override
  public Map<String, Document> getAllXmlInRoot() {
    return null;
  }

  @Nullable
  @Override
  public IdeVersion getSinceBuild() {
    return null;
  }

  @Nullable
  @Override
  public IdeVersion getUntilBuild() {
    return null;
  }

  @Override
  public boolean isCompatibleWithIde(@NotNull String ideVersion) {
    return false;
  }

  @NotNull
  @Override
  public List<PluginDependency> getDependencies() {
    return null;
  }

  @NotNull
  @Override
  public List<PluginDependency> getModuleDependencies() {
    return null;
  }

  @NotNull
  @Override
  public String getPluginName() {
    return null;
  }

  @NotNull
  @Override
  public String getPluginVersion() {
    return null;
  }

  @NotNull
  @Override
  public String getPluginId() {
    return null;
  }

  @NotNull
  @Override
  public String getPluginVendor() {
    return null;
  }

  @NotNull
  @Override
  public Set<String> getDefinedModules() {
    return null;
  }

  @NotNull
  @Override
  public ClassPool getPluginClassPool() {
    return null;
  }

  @NotNull
  @Override
  public ClassPool getLibraryClassPool() {
    return null;
  }

  @NotNull
  @Override
  public ClassPool getAllClassesPool() {
    return null;
  }
}
