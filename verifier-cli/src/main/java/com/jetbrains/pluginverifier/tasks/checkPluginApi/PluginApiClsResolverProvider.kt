package com.jetbrains.pluginverifier.tasks.checkPluginApi

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.pluginverifier.ResultHolder
import com.jetbrains.pluginverifier.createPluginResolver
import com.jetbrains.pluginverifier.parameters.jdk.JdkDescriptorsCache
import com.jetbrains.pluginverifier.parameters.jdk.JdkPath
import com.jetbrains.pluginverifier.plugin.PluginDetails
import com.jetbrains.pluginverifier.reporting.verification.PluginVerificationReportage
import com.jetbrains.pluginverifier.repository.cache.ResourceCacheEntryResult
import com.jetbrains.pluginverifier.verifiers.resolution.ClsResolver
import com.jetbrains.pluginverifier.verifiers.resolution.ClsResolverProvider
import com.jetbrains.pluginverifier.verifiers.resolution.PluginApiClsResolver
import java.io.Closeable

class PluginApiClsResolverProvider(private val jdkDescriptorCache: JdkDescriptorsCache,
                                   private val jdkPath: JdkPath,
                                   private val basePluginResolver: Resolver) : ClsResolverProvider {

  override fun create(pluginDetails: PluginDetails,
                      resultHolder: ResultHolder,
                      reportage: PluginVerificationReportage): ClsResolver {
    val pluginResolver = pluginDetails.pluginClassesLocations.createPluginResolver()
    return with(jdkDescriptorCache.getJdkResolver(jdkPath)) {
      when (this) {
        is ResourceCacheEntryResult.Found -> {
          val jdkClassesResolver = resourceCacheEntry.resource.jdkClassesResolver
          val closeableResources = listOf<Closeable>(resourceCacheEntry)
          PluginApiClsResolver(pluginResolver, basePluginResolver, jdkClassesResolver, closeableResources)
        }
        is ResourceCacheEntryResult.Failed -> throw IllegalStateException("Unable to resolve JDK descriptor", error)
        is ResourceCacheEntryResult.NotFound -> throw IllegalStateException("Unable to find JDK $jdkPath: $message")
      }
    }
  }
}