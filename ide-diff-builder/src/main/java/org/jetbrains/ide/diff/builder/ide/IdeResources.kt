/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.ide.diff.builder.ide

import com.jetbrains.plugin.structure.base.utils.closeAll
import com.jetbrains.plugin.structure.base.utils.closeOnException
import com.jetbrains.plugin.structure.classes.resolvers.CompositeResolver
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.ide.classes.IdeResolverCreator
import com.jetbrains.plugin.structure.intellij.classes.locator.CompileServerExtensionKey
import com.jetbrains.plugin.structure.intellij.classes.plugin.ClassSearchContext
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesFinder
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesLocations
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import org.slf4j.LoggerFactory
import java.io.Closeable

class IdeResources(
  val ide: Ide,
  platformResolver: Resolver,
  bundledPluginResolvers: List<Resolver>,
  private val closeableResources: List<Closeable>
) : Closeable {

  val allResolver: Resolver = CompositeResolver.create(listOf(platformResolver) + bundledPluginResolvers)

  override fun close() {
    closeableResources.closeAll()
  }
}

fun buildIdeResources(ide: Ide, readMode: Resolver.ReadMode): IdeResources {
  val closeableResources = arrayListOf<Closeable>()
  closeableResources.closeOnException {
    val platformResolver = IdeResolverCreator.createIdeResolver(readMode, ide)
    closeableResources += platformResolver

    val pluginClassLocations = readBundledPluginsClassesLocations(ide, readMode)
    closeableResources += pluginClassLocations

    val bundledPluginsResolvers = pluginClassLocations.flatMap { it.getPluginClassesResolver() }

    return IdeResources(ide, platformResolver, bundledPluginsResolvers, closeableResources)
  }
}

private val LOG = LoggerFactory.getLogger("ide-resources")

/**
 * IDs of plugins to be ignored from processing. Their APIs are not relevant to IDE.
 */
private val IGNORED_PLUGIN_IDS = setOf("org.jetbrains.kotlin", "org.jetbrains.android")

/**
 * Specifies which classes should be put into the plugin's class files resolver.
 * Currently, we select all the classes from:
 * 1) for `.jar`-red plugin, all classes contained in the `.jar`
 * 2) for directory-based plugins, all classes from the `/lib/` directory and
 * from the `/classes` directory, if any
 * 3) JPS-used classes, such as `Kotlin/lib/jps`.
 */
private val pluginClassesLocationsKeys = IdePluginClassesFinder.MAIN_CLASSES_KEYS + listOf(CompileServerExtensionKey)

private fun readBundledPluginsClassesLocations(ide: Ide, readMode: Resolver.ReadMode): List<IdePluginClassesLocations> =
  ide.bundledPlugins.mapNotNull { readPluginClassesExceptionally(it, readMode) }

private fun readPluginClassesExceptionally(idePlugin: IdePlugin, readMode: Resolver.ReadMode): IdePluginClassesLocations? {
  if (idePlugin.pluginId in IGNORED_PLUGIN_IDS) {
    return null
  }
  LOG.debug("Reading class files of a bundled plugin $idePlugin  (${idePlugin.originalFile})")
  return IdePluginClassesFinder.findPluginClasses(idePlugin, readMode, pluginClassesLocationsKeys, ClassSearchContext.DEFAULT)
}

private fun IdePluginClassesLocations.getPluginClassesResolver(): List<Resolver> =
  pluginClassesLocationsKeys.flatMap { getResolvers(it) }