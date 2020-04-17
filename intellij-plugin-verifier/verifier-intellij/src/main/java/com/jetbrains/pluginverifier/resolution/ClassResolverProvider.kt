/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.resolution

import com.jetbrains.plugin.structure.base.utils.closeAll
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.plugin.PluginDetails
import com.jetbrains.pluginverifier.verifiers.packages.PackageFilter
import java.io.Closeable

/**
 * Provides a concrete implementation of [Resolver] used for the current verification.
 */
interface ClassResolverProvider {

  fun provide(checkedPluginDetails: PluginDetails): Result

  fun provideExternalClassesPackageFilter(): PackageFilter

  data class Result(
    val pluginResolver: Resolver,
    val allResolver: Resolver,
    val dependenciesGraph: DependenciesGraph,
    private val closeableResources: List<Closeable>
  ) : Closeable {
    override fun close() {
      closeableResources.closeAll()
    }
  }

}