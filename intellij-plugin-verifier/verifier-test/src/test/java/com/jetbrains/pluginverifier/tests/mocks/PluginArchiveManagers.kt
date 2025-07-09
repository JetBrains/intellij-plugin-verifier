/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.tests.mocks

import com.jetbrains.plugin.structure.intellij.plugin.PluginArchiveManager
import org.junit.rules.TemporaryFolder

internal fun TemporaryFolder.createPluginArchiveManager(): PluginArchiveManager {
  return PluginArchiveManager(this.newFolder("extracted-plugins").toPath())
}