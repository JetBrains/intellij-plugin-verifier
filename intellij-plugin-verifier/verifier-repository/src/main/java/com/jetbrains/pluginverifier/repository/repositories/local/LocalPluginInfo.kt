/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.repository.repositories.local

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.WithIdePlugin
import java.io.ObjectInputStream

/**
 * Identifier of a local plugin.
 *
 * @see [LocalPluginRepository]
 */
class LocalPluginInfo(
  override val idePlugin: IdePlugin
) : PluginInfo(
  idePlugin.pluginId!!,
  idePlugin.pluginName ?: idePlugin.pluginId!!,
  idePlugin.pluginVersion!!,
  idePlugin.sinceBuild,
  idePlugin.untilBuild,
  idePlugin.vendor
), WithIdePlugin {

  override val presentableName
    get() = idePlugin.toString()

  private fun writeReplace(): Any = throw UnsupportedOperationException("Local plugins cannot be serialized")

  @Suppress("UNUSED_PARAMETER")
  private fun readObject(stream: ObjectInputStream): Unit = throw UnsupportedOperationException("Local plugins cannot be deserialized")

  override fun equals(other: Any?) = other is LocalPluginInfo && idePlugin == other.idePlugin

  override fun hashCode() = idePlugin.hashCode()

}