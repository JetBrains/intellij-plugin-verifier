/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

public interface PluginDependency extends Serializable {
  @NotNull
  String getId();

  boolean isOptional();

  boolean isModule();
}
