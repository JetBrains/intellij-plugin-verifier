/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.filtering

import com.jetbrains.plugin.structure.classes.resolvers.AbstractJarResolver
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.intellij.classes.locator.CompileServerExtensionKey
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesLocations

/**
 * [ClassesSelector] that selects classes used for the external build processes,
 * such as JPS classes bundled into the Kotlin plugin (`/lib/jps`).
 */
class ExternalBuildClassesSelector : ClassesSelector {
  override fun getClassLoader(classesLocations: IdePluginClassesLocations): List<Resolver> =
    classesLocations.getResolvers(CompileServerExtensionKey)

  override fun getClassesForCheck(classesLocations: IdePluginClassesLocations): Set<String> {
    val compileServerResolvers = classesLocations.getResolvers(CompileServerExtensionKey)
    val jarFileResolvers = compileServerResolvers.filterIsInstance<AbstractJarResolver>()

    val allServiceImplementations = hashSetOf<String>()
    for (jarFileResolver in jarFileResolvers) {
      jarFileResolver.implementedServiceProviders
        .filterKeys { isJetbrainsServiceProvider(it) }
        .flatMapTo(allServiceImplementations) { it.value }
    }

    return compileServerResolvers
      .filter { resolver ->
        allServiceImplementations.any { serviceImplementation -> resolver.containsClass(serviceImplementation.replace('.', '/')) }
      }
      .flatMapTo(hashSetOf()) { it.allClasses }
  }

  private fun isJetbrainsServiceProvider(serviceProvider: String): Boolean =
    serviceProvider.startsWith("org.jetbrains.")
}