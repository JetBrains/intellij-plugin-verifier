package com.jetbrains.plugin.structure.ide;

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin;
import com.jetbrains.plugin.structure.intellij.version.IdeVersion;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Collections;
import java.util.List;

class IdeImpl extends Ide {
  private final List<IdePlugin> myBundledPlugins;

  private final IdeVersion myVersion;
  private final File myIdePath;

  IdeImpl(@NotNull File idePath,
          @NotNull IdeVersion version,
          @NotNull List<IdePlugin> bundledPlugins) {
    this(idePath, bundledPlugins, version);
  }

  private IdeImpl(@NotNull File idePath, @NotNull List<IdePlugin> bundledPlugins, @NotNull IdeVersion version) {
    myIdePath = idePath;
    myBundledPlugins = bundledPlugins;
    myVersion = version;
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
}
