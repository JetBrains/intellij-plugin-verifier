package com.jetbrains.pluginverifier.resolution

import com.jetbrains.plugin.structure.base.utils.closeOnException
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.classes.resolvers.CompositeResolver
import com.jetbrains.pluginverifier.createPluginResolver
import com.jetbrains.pluginverifier.parameters.jdk.JdkDescriptorsCache
import com.jetbrains.pluginverifier.plugin.PluginDetails
import com.jetbrains.pluginverifier.repository.cache.ResourceCacheEntryResult
import com.jetbrains.pluginverifier.results.VerificationResult
import com.jetbrains.pluginverifier.verifiers.packages.NegatedPackageFilter
import com.jetbrains.pluginverifier.verifiers.packages.PackageFilter
import com.jetbrains.pluginverifier.verifiers.resolution.*
import java.io.Closeable
import java.nio.file.Path

class PluginApiClassResolverProvider(
    private val jdkDescriptorCache: JdkDescriptorsCache,
    private val jdkPath: Path,
    private val basePluginResolver: Resolver,
    private val basePluginPackageFilter: PackageFilter
) : ClassResolverProvider {

  override fun provide(
      checkedPluginDetails: PluginDetails,
      verificationResult: VerificationResult
  ): ClassResolverProvider.Result {
    val closeableResources = arrayListOf<Closeable>()
    closeableResources.closeOnException {
      val checkedPluginClassResolver = checkedPluginDetails.pluginClassesLocations.createPluginResolver()

      return with(jdkDescriptorCache.getJdkResolver(jdkPath)) {
        when (this) {
          is ResourceCacheEntryResult.Found -> {
            closeableResources += resourceCacheEntry

            val jdkClassResolver = resourceCacheEntry.resource.jdkResolver

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
            val resolver = CompositeResolver.create(checkedPluginClassResolver, basePluginResolver, jdkClassResolver).caching()

            ClassResolverProvider.Result(resolver, closeableResources)
          }
          is ResourceCacheEntryResult.Failed -> throw IllegalStateException("Unable to resolve JDK descriptor", error)
          is ResourceCacheEntryResult.NotFound -> throw IllegalStateException("Unable to find JDK $jdkPath: $message")
        }
      }
    }
  }

  override fun provideExternalClassesPackageFilter() = NegatedPackageFilter(basePluginPackageFilter)

}