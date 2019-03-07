package com.jetbrains.pluginverifier.verifiers.resolution

import com.jetbrains.plugin.structure.base.utils.closeLogged
import com.jetbrains.plugin.structure.classes.resolvers.CacheResolver
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.classes.resolvers.UnionResolver
import com.jetbrains.pluginverifier.parameters.packages.PackageFilter
import java.io.Closeable

/**
 * Default implementation of [ClassResolver] that resolves classes in the following order:
 * 1) Verified plugin
 * 2) JDK classes
 * 3) IDE classes
 * 4) Class of plugin's dependencies
 */
class DefaultClassResolver(
    private val pluginResolver: Resolver,
    private val dependenciesResolver: Resolver,
    private val jdkClassesResolver: Resolver,
    private val ideResolver: Resolver,
    private val externalClassesPackageFilter: PackageFilter,
    private val closeableResources: List<Closeable>
) : ClassResolver {

  private val cachingResolver = CacheResolver(
      UnionResolver.create(
          listOf(
              pluginResolver,
              jdkClassesResolver,
              ideResolver,
              dependenciesResolver
          )
      )
  )

  override fun isExternalClass(className: String) = externalClassesPackageFilter.accept(className)

  override fun classExists(className: String) = getOriginOfClass(className) != null

  override fun packageExists(packageName: String) = cachingResolver.containsPackage(packageName)

  override fun resolveClass(className: String): ClassResolution {
    if (isExternalClass(className)) {
      return ClassResolution.ExternalClass
    }
    return cachingResolver.resolveClassSafely(className)
  }

  override fun getOriginOfClass(className: String): ClassFileOrigin? {
    if (pluginResolver.containsClass(className)) {
      return ClassFileOrigin.PLUGIN_INTERNAL_CLASS
    }
    if (jdkClassesResolver.containsClass(className)) {
      return ClassFileOrigin.JDK_CLASS
    }
    if (ideResolver.containsClass(className)) {
      return ClassFileOrigin.IDE_CLASS
    }
    if (dependenciesResolver.containsClass(className)) {
      return ClassFileOrigin.CLASS_OF_PLUGIN_DEPENDENCY
    }
    return null
  }

  override fun close() {
    closeableResources.forEach { it.closeLogged() }
  }

}