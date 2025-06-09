/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.loaders

import com.jetbrains.plugin.structure.base.plugin.ThirdPartyDependency
import com.jetbrains.plugin.structure.base.plugin.parseThirdPartyDependenciesByPath
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager.Companion.META_INF
import java.nio.file.Path

private val THIRD_PARTY_LIBRARIES_FILE_NAME = "dependencies.json"

class ThirdPartyDependencyLoader : PluginDirectoryResourceLoader<ThirdPartyDependency> {
  override fun load(pluginDirectory: Path): List<ThirdPartyDependency> {
    val path = pluginDirectory.resolve(META_INF).resolve(THIRD_PARTY_LIBRARIES_FILE_NAME)
    return parseThirdPartyDependenciesByPath(path)
  }
}