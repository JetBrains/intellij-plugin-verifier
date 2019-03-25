package com.jetbrains.pluginverifier.verifiers.resolution

import com.jetbrains.plugin.structure.base.utils.closeLogged
import com.jetbrains.plugin.structure.classes.resolvers.CacheResolver
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.classes.resolvers.UnionResolver
import com.jetbrains.pluginverifier.parameters.packages.PackageFilter
import java.io.Closeable

/**
 * [ClassResolver] that resolves classes in the following order:
 * 1) Verified plugin
 * 2) Classes of a plugin against which the plugin is verified
 * 3) Classes of JDK
 *
 * A class is considered external if:
 * 1) [basePluginResolver] doesn't contain it in class files
 * 2) [basePluginPackageFilter] rejects it, meaning that
 * the class is not supposed to reside in the base plugin
 *
 * For instance, if the class is expected to reside in the base plugin
 * and is not resolved among its classes, a "Class not found" problem
 * will be reported.
 */
class PluginApiClassResolver(
    private val checkedPluginResolver: Resolver,
    private val basePluginResolver: Resolver,
    private val jdkClassesResolver: Resolver,
    private val closeableResources: List<Closeable>,
    private val basePluginPackageFilter: PackageFilter
) : ClassResolver {

  private val cachingResolver = CacheResolver(
      UnionResolver.create(
          listOf(checkedPluginResolver, basePluginResolver, jdkClassesResolver)
      )
  )

  override fun isExternalClass(className: String) =
      !basePluginPackageFilter.accept(className) && !cachingResolver.containsClass(className)

  override fun classExists(className: String) = cachingResolver.containsClass(className)

  override fun packageExists(packageName: String) = cachingResolver.containsPackage(packageName)

  override fun resolveClass(className: String): ClassResolution {
    if (isExternalClass(className)) {
      return ClassResolution.ExternalClass
    }
    return cachingResolver.resolveClassSafely(className)
  }

  override fun getOriginOfClass(className: String): ClassFileOrigin? {
    val checkPluginLocation = checkedPluginResolver.getClassLocation(className)
    if (checkPluginLocation != null) {
      return ClassFileOrigin.PluginClass(checkPluginLocation)
    }
    val basePluginLocation = basePluginResolver.getClassLocation(className)
    if (basePluginLocation != null) {
      return ClassFileOrigin.ClassOfPluginDependency
    }
    val jdkLocation = jdkClassesResolver.getClassLocation(className)
    if (jdkLocation != null) {
      return ClassFileOrigin.JdkClass
    }
    return null
  }

  override fun close() {
    closeableResources.forEach { it.closeLogged() }
  }

}