/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide.jps

import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import java.nio.file.Path

class CompiledIde(private val idePath: Path, private val version: IdeVersion, private val bundledPlugins: List<IdePlugin>) : Ide() {
  override fun getVersion(): IdeVersion = version

  override fun getBundledPlugins(): List<IdePlugin> = bundledPlugins

  override fun getIdePath(): Path = idePath
}