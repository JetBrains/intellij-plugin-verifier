/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.plugin

import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.utils.closeLogged
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesLocations
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.files.FileLock
import java.io.Closeable

/**
 * Holder of plugin's resources necessary for verification.
 */
class PluginDetails(

  /**
   * Identifier of a verified plugin.
   */
  val pluginInfo: PluginInfo,

  /**
   * IDE plugin being verified.
   */
  val idePlugin: IdePlugin,

  /**
   * Warnings of the plugin structure.
   */
  val pluginWarnings: List<PluginProblem>,

  /**
   * Accessor of classes of the verified plugin.
   */
  val pluginClassesLocations: IdePluginClassesLocations,

  /**
   * [File lock] [FileLock] registered for the plugin's file
   * which guarantees that the file will not be deleted
   * while it is used.
   */
  private val pluginFileLock: FileLock?

) : Closeable {

  override fun close() {
    pluginClassesLocations.closeLogged()
    pluginFileLock.closeLogged()
  }

  override fun toString() = "PluginDetails($pluginInfo)"

}