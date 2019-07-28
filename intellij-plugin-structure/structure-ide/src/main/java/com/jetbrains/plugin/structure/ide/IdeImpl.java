package com.jetbrains.plugin.structure.ide;

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin;
import com.jetbrains.plugin.structure.intellij.version.IdeVersion;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;

class IdeImpl extends Ide {
  private final List<IdePlugin> myBundledPlugins;

  private final IdeVersion myVersion;
  private final File myIdePath;
  private final Set<PluginIdAndVersion> myIncompatiblePlugins;

  IdeImpl(@NotNull File idePath,
          @NotNull IdeVersion version,
          @NotNull List<IdePlugin> bundledPlugins,
          @NotNull Set<PluginIdAndVersion> incompatiblePlugins) {
    myIdePath = idePath;
    myBundledPlugins = bundledPlugins;
    myVersion = version;
    myIncompatiblePlugins = incompatiblePlugins;
  }

  @NotNull
  @Override
  public IdeVersion getVersion() {
    return myVersion;
  }

  @Override
  @NotNull
  public List<IdePlugin> getBundledPlugins() {
    return Collections.unmodifiableList(myBundledPlugins);
  }

  @NotNull
  @Override
  public File getIdePath() {
    return myIdePath;
  }

  @Override
  public String toString() {
    return myVersion.asString();
  }

  @NotNull
  @Override
  public Set<PluginIdAndVersion> getIncompatiblePlugins() {
    return Collections.unmodifiableSet(myIncompatiblePlugins);
  }
}
