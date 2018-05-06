package com.jetbrains.pluginverifier.verifiers.resolution

import com.jetbrains.plugin.structure.classes.resolvers.CacheResolver
import com.jetbrains.plugin.structure.classes.resolvers.InvalidClassFileException
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.classes.resolvers.UnionResolver
import com.jetbrains.pluginverifier.misc.closeLogged
import java.io.Closeable

/**
 * Default implementation of [ClsResolver] that resolves classes in the following order:
 * 1) Verified plugin
 * 2) JDK classes
 * 3) IDE classes
 * 4) Class of plugin's dependencies
 */
class DefaultClsResolver(private val pluginResolver: Resolver,
                         private val dependenciesResolver: Resolver,
                         private val jdkClassesResolver: Resolver,
                         private val ideResolver: Resolver,
                         private val externalClassesPrefixes: List<String>,
                         private val closeableResources: List<Closeable>) : ClsResolver {

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

  override fun isExternalClass(className: String) = externalClassesPrefixes.any { it.isNotEmpty() && className.startsWith(it) }

  override fun classExists(className: String) = getOriginOfClass(className) != null

  override fun resolveClass(className: String): ClsResolution {
    if (isExternalClass(className)) {
      return ClsResolution.ExternalClass
    }

    return try {
      cachingResolver.findClass(className)?.let { ClsResolution.Found(it) } ?: ClsResolution.NotFound
    } catch (e: InvalidClassFileException) {
      ClsResolution.InvalidClassFile(e.asmError)
    } catch (e: Exception) {
      ClsResolution.FailedToReadClassFile(e.message ?: e.javaClass.name)
    }
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