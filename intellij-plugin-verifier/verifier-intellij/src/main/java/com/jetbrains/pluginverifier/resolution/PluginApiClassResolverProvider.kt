/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.resolution

import com.jetbrains.plugin.structure.base.utils.closeOnException
import com.jetbrains.plugin.structure.classes.resolvers.CompositeResolver
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependencyImpl
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.id
import com.jetbrains.pluginverifier.createPluginResolver
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.dependencies.DependencyEdge
import com.jetbrains.pluginverifier.dependencies.DependencyNode
import com.jetbrains.pluginverifier.dependencies.DependencyNode.Companion.dependencyNode
import com.jetbrains.pluginverifier.jdk.JdkDescriptor
import com.jetbrains.pluginverifier.plugin.PluginDetails
import com.jetbrains.pluginverifier.verifiers.packages.NegatedPackageFilter
import com.jetbrains.pluginverifier.verifiers.packages.PackageFilter
import com.jetbrains.pluginverifier.verifiers.resolution.caching
import java.io.Closeable

class PluginApiClassResolverProvider(
  private val jdkDescriptor: JdkDescriptor,
  private val basePluginDetails: PluginDetails,
  private val basePluginPackageFilter: PackageFilter
) : ClassResolverProvider {

  override fun provide(checkedPluginDetails: PluginDetails): ClassResolverProvider.Result {
    val closeableResources = arrayListOf<Closeable>()
    closeableResources.closeOnException {
      val checkedPluginClassResolver =
        checkedPluginDetails.pluginClassesLocations.createPluginResolver(checkedPluginDetails.pluginInfo.pluginId)
      val basePluginResolver = basePluginDetails.pluginClassesLocations.createPluginResolver()

      /**
       * Resolves classes in the following order:
       * 1) Verified plugin
       * 2) Classes of a plugin against which the plugin is verified
       * 3) Classes of JDK
       *
       * A class is considered external if:
       * 1) [basePluginResolver] doesn't contain it in class files
       * 2) [basePluginPackageFilter] rejects it, meaning that the class does not reside in the base plugin
       *
       * For instance, if the class is expected to reside in the base plugin and is not resolved among its classes,
       * a "Class not found" problem will be reported.
       */
      val resolver = CompositeResolver.create(checkedPluginClassResolver, basePluginResolver, jdkDescriptor.jdkResolver).caching()

      val checkedPluginNode = newDependencyNode(checkedPluginDetails)
      val basePluginNode = newDependencyNode(basePluginDetails)

      val dependenciesGraph = DependenciesGraph(
        checkedPluginNode,
        setOf(checkedPluginNode, basePluginNode),
        setOf(
          DependencyEdge(
            checkedPluginNode,
            basePluginNode,
            PluginDependencyImpl(basePluginNode.id, false, false)
          )
        ),
        emptyMap()
      )

      return ClassResolverProvider.Result(
        checkedPluginClassResolver,
        resolver,
        dependenciesGraph,
        closeableResources
      )
    }
  }

  override fun provideExternalClassesPackageFilter() = NegatedPackageFilter(basePluginPackageFilter)

  private fun newDependencyNode(pluginDetails: PluginDetails): DependencyNode {
    val infoPluginId = pluginDetails.pluginInfo.pluginId
    val plugin = pluginDetails.idePlugin
    return if (infoPluginId != plugin.id) {
      DependencyNode.AliasedPluginDependency(infoPluginId, plugin)
    } else {
      dependencyNode(plugin)
    }
  }
}