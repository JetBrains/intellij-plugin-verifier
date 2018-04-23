package com.jetbrains.pluginverifier.plugin

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesLocations
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.pluginverifier.misc.closeLogged
import com.jetbrains.pluginverifier.repository.files.FileLock
import java.io.Closeable

/**
 * Holder of plugin's resources necessary for the verification.
 */
data class PluginDetails(

    /**
     * The [IDE plugin] [IdePlugin] being verified.
     */
    val plugin: IdePlugin,

    /**
     * [Warnings] [PluginProblem.Level.WARNING] of the plugin structure.
     */
    val pluginWarnings: List<PluginProblem>,

    /**
     * Accessor of classes of the verified [plugin].
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

  override fun toString() = "PluginDetails($plugin)"

}