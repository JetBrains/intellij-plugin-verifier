package com.jetbrains.pluginverifier.tests.mocks

import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesLocations
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.plugin.PluginDetails
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.files.FileLock
import com.jetbrains.pluginverifier.repository.files.IdleFileLock
import com.jetbrains.pluginverifier.repository.repositories.bundled.BundledPluginInfo
import org.objectweb.asm.tree.ClassNode
import java.io.Closeable
import java.nio.file.Files
import java.nio.file.Path

internal val IdePlugin.emptyClassesLocations
  get() = IdePluginClassesLocations(this, allocatedResource = Closeable {}, locations = emptyMap())

internal fun IdePlugin.emptyLock(lockPath: Path): FileLock = IdleFileLock(lockPath)

internal fun IdePlugin.emptyLock(): FileLock = emptyLock(Files.createTempFile("ide-plugin", ".lock"))

internal val IdePlugin.pluginInfo
  get() = createMockPluginInfo(pluginId!!, pluginVersion!!)

internal fun IdePlugin.bundledPluginInfo(ideVersion: IdeVersion): PluginInfo = BundledPluginInfo(ideVersion, this)

internal fun IdePlugin.getDetails(classesLocations: IdePluginClassesLocations = emptyClassesLocations): PluginDetails =
  PluginDetails(pluginInfo, idePlugin = this, pluginWarnings = emptyList(), classesLocations, emptyLock())

internal fun IdePlugin.getDetails(pluginInfo: PluginInfo): PluginDetails =
  PluginDetails(pluginInfo, idePlugin = this, pluginWarnings = emptyList(), emptyClassesLocations, emptyLock())

internal fun bundledPluginClassesLocation(plugin: IdePlugin, classNodes: List<ClassNode>): IdePluginClassesLocations {
  val pluginIdString = plugin.pluginId ?: "unknown plugin ID"
  val classesLocator = MockClassesLocator(pluginIdString, classNodes)
 val locationKey = MockLocationKey(pluginIdString, classesLocator)

  return IdePluginClassesLocations(
    plugin,
    allocatedResource = Closeable {},
    locations = mapOf(locationKey to classesLocator.getClassResolvers())
  )
}

