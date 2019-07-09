package com.jetbrains.pluginverifier.resolution

import com.jetbrains.plugin.structure.base.utils.closeAll
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.pluginverifier.plugin.PluginDetails
import com.jetbrains.pluginverifier.results.VerificationResult
import com.jetbrains.pluginverifier.verifiers.packages.PackageFilter
import java.io.Closeable

/**
 * Provides a concrete implementation of [Resolver] used for the current verification.
 */
interface ClassResolverProvider {

  fun provide(checkedPluginDetails: PluginDetails, verificationResult: VerificationResult): Result

  fun provideExternalClassesPackageFilter(): PackageFilter

  data class Result(
      val classResolver: Resolver,
      private val closeableResources: List<Closeable>
  ) : Closeable {
    override fun close() {
      closeableResources.closeAll()
    }
  }

}