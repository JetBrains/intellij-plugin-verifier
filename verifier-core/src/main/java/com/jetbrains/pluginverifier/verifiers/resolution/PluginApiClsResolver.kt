package com.jetbrains.pluginverifier.verifiers.resolution

import com.jetbrains.plugin.structure.classes.resolvers.CacheResolver
import com.jetbrains.plugin.structure.classes.resolvers.InvalidClassFileException
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.classes.resolvers.UnionResolver
import com.jetbrains.pluginverifier.misc.closeLogged
import com.jetbrains.pluginverifier.parameters.packages.PackageFilter
import java.io.Closeable

/**
 * [ClsResolver] that resolves classes in the following order:
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
class PluginApiClsResolver(private val checkedPluginResolver: Resolver,
                           private val basePluginResolver: Resolver,
                           private val jdkClassesResolver: Resolver,
                           private val closeableResources: List<Closeable>,
                           private val basePluginPackageFilter: PackageFilter) : ClsResolver {

  private val cachingResolver = CacheResolver(
      UnionResolver.create(
          listOf(checkedPluginResolver, basePluginResolver, jdkClassesResolver)
      )
  )

  override fun isExternalClass(className: String) =
      !basePluginPackageFilter.accept(className) && !cachingResolver.containsClass(className)

  override fun classExists(className: String) = getOriginOfClass(className) != null

  override fun resolveClass(className: String): ClsResolution {
    if (isExternalClass(className)) {
      return ClsResolution.ExternalClass
    }

    return try {
      cachingResolver.findClass(className)
          ?.let { ClsResolution.Found(it) }
          ?: ClsResolution.NotFound
    } catch (e: InvalidClassFileException) {
      ClsResolution.InvalidClassFile(e.asmError)
    } catch (e: Exception) {
      ClsResolution.FailedToReadClassFile(e.message ?: e.javaClass.name)
    }
  }

  override fun getOriginOfClass(className: String): ClassFileOrigin? {
    if (checkedPluginResolver.containsClass(className)) {
      return ClassFileOrigin.PLUGIN_INTERNAL_CLASS
    }
    if (basePluginResolver.containsClass(className)) {
      return ClassFileOrigin.CLASS_OF_PLUGIN_DEPENDENCY
    }
    if (jdkClassesResolver.containsClass(className)) {
      return ClassFileOrigin.JDK_CLASS
    }
    return null
  }

  override fun close() {
    closeableResources.forEach { it.closeLogged() }
  }

}