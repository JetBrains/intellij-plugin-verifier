package com.jetbrains.pluginverifier.plugin

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesLocations
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.pluginverifier.misc.closeLogged
import com.jetbrains.pluginverifier.repository.FileLock
import java.io.Closeable

sealed class PluginDetails : Closeable {

  open val plugin: IdePlugin? = null

  open val pluginClassesLocations: IdePluginClassesLocations? = null

  open val warnings: List<PluginProblem>? = null

  override fun close() = Unit

  data class ByFileLock(override val plugin: IdePlugin,
                        override val pluginClassesLocations: IdePluginClassesLocations,
                        override val warnings: List<PluginProblem>,
                        private val pluginLock: FileLock) : PluginDetails() {
    override fun close() {
      pluginLock.closeLogged()
      pluginClassesLocations.closeLogged()
    }
  }

  data class FoundOpenPluginAndClasses(override val plugin: IdePlugin,
                                       override val pluginClassesLocations: IdePluginClassesLocations,
                                       override val warnings: List<PluginProblem>) : PluginDetails()

  data class FoundOpenPluginWithoutClasses(override val plugin: IdePlugin) : PluginDetails()

  data class BadPlugin(val pluginErrorsAndWarnings: List<PluginProblem>) : PluginDetails()

  data class FailedToDownload(val reason: String) : PluginDetails()

  data class NotFound(val reason: String) : PluginDetails()
}