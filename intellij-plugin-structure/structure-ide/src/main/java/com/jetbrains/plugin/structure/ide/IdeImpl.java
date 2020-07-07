/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide;

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin;
import com.jetbrains.plugin.structure.intellij.version.IdeVersion;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;

class IdeImpl extends Ide {
  private final List<IdePlugin> myBundledPlugins;

  private final IdeVersion myVersion;
  private final Path myIdePath;
  private final Set<PluginIdAndVersion> myIncompatiblePlugins;

  IdeImpl(@NotNull Path idePath,
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
  public Path getIdePath() {
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
