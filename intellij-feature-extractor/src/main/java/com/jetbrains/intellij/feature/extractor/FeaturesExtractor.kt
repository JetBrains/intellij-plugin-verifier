/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.intellij.feature.extractor

import com.jetbrains.intellij.feature.extractor.FeaturesExtractor.extractFeatures
import com.jetbrains.intellij.feature.extractor.extractor.*
import com.jetbrains.plugin.structure.base.utils.closeLogged
import com.jetbrains.plugin.structure.classes.resolvers.CompositeResolver
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesFinder
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesLocations
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import java.io.Closeable

/**
 * Main feature extractor entry point.
 *
 * Use [extractFeatures] to extract the plugin features. The class analyses the
 * plugin class-files. Some classes may refer to the platform API constant.
 * This is why the method also takes IDE build (presumably with which the plugin is compatible) as parameter.
 */
object FeaturesExtractor {

  private val ALL_EXTRACTORS = listOf(
    RunConfigurationExtractor(),
    FacetTypeExtractor(),
    FileTypeFactoryExtractor(),
    FileTypeExtractor(),
    ArtifactTypeExtractor(),
    ModuleTypeExtractor(),
    DependencySupportExtractor()
  )

  fun extractFeatures(ide: Ide, ideResolver: Resolver, plugin: IdePlugin): List<ExtensionPointFeatures> {
    val bundledClassLocations = ide.bundledPlugins.map { IdePluginClassesFinder.findPluginClasses(it) }
    Closeable { bundledClassLocations.forEach { it.closeLogged() } }.use {
      val bundledResolvers = bundledClassLocations.map { it.constructMainPluginResolver() }
      IdePluginClassesFinder.findPluginClasses(plugin).use { pluginClassesLocations ->
        val pluginResolver = pluginClassesLocations.constructMainPluginResolver()
        //don't close this resolver, because ideResolver will be closed by the caller.
        val resolver = CompositeResolver.create(listOf(pluginResolver, ideResolver) + bundledResolvers)
        return ALL_EXTRACTORS.flatMap { it.extract(plugin, resolver) }
      }
    }
  }

  private fun IdePluginClassesLocations.constructMainPluginResolver(): Resolver =
    CompositeResolver.create(IdePluginClassesFinder.MAIN_CLASSES_KEYS.flatMap { getResolvers(it) })

}