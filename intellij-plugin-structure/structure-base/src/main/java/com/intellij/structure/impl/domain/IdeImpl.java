package com.intellij.structure.impl.domain;

import com.intellij.structure.ide.Ide;
import com.intellij.structure.ide.IdeVersion;
import com.intellij.structure.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class IdeImpl extends Ide {
  private final List<Plugin> myBundledPlugins;
  private final List<Plugin> myCustomPlugins;

  private final IdeVersion myVersion;
  private final File myIdePath;

  IdeImpl(@NotNull File idePath,
          @NotNull IdeVersion version,
          @NotNull List<Plugin> bundledPlugins) throws IOException {
    this(idePath, bundledPlugins, Collections.<Plugin>emptyList(), version);
  }

  private IdeImpl(@NotNull File idePath, @NotNull List<Plugin> bundledPlugins, @NotNull List<Plugin> customPlugins, @NotNull IdeVersion version) {
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
  public Ide getExpandedIde(@NotNull Plugin plugin) {
    List<Plugin> newCustoms = new ArrayList<Plugin>(myCustomPlugins);
    newCustoms.add(plugin);
    return new IdeImpl(myIdePath, myBundledPlugins, newCustoms, myVersion);
  }

  @Override
  @NotNull
  public List<Plugin> getCustomPlugins() {
    return Collections.unmodifiableList(myCustomPlugins);
  }

  @Override
  @NotNull
  public List<Plugin> getBundledPlugins() {
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
