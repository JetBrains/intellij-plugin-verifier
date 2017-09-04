package com.jetbrains.plugin.structure.ide;

import com.jetbrains.plugin.structure.intellij.version.IdeVersion;
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class IdeImpl extends Ide {
  private final List<IdePlugin> myBundledPlugins;
  private final List<IdePlugin> myCustomPlugins;

  private final IdeVersion myVersion;
  private final File myIdePath;

  IdeImpl(@NotNull File idePath,
          @NotNull IdeVersion version,
          @NotNull List<IdePlugin> bundledPlugins) {
    this(idePath, bundledPlugins, Collections.<IdePlugin>emptyList(), version);
  }

  private IdeImpl(@NotNull File idePath, @NotNull List<IdePlugin> bundledPlugins, @NotNull List<IdePlugin> customPlugins, @NotNull IdeVersion version) {
    myIdePath = idePath;
    myBundledPlugins = bundledPlugins;
    myCustomPlugins = customPlugins;
    myVersion = version;
  }

  @NotNull
  @Override
  public IdeVersion getVersion() {
    return myVersion;
  }

  @NotNull
  @Override
  public Ide getExpandedIde(@NotNull IdePlugin plugin) {
    List<IdePlugin> newCustoms = new ArrayList<IdePlugin>(myCustomPlugins);
    newCustoms.add(plugin);
    return new IdeImpl(myIdePath, myBundledPlugins, newCustoms, myVersion);
  }

  @Override
  @NotNull
  public List<IdePlugin> getCustomPlugins() {
    return Collections.unmodifiableList(myCustomPlugins);
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
