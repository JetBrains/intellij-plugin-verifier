package com.jetbrains.pluginverifier.verifiers.resolution

import com.jetbrains.plugin.structure.classes.resolvers.CacheResolver
import com.jetbrains.plugin.structure.classes.resolvers.InvalidClassFileException
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.classes.resolvers.UnionResolver

class DefaultClsResolver(private val pluginResolver: Resolver,
                         private val dependenciesResolver: Resolver,
                         private val jdkClassesResolver: Resolver,
                         private val ideResolver: Resolver,
                         private val externalClassesPrefixes: List<String>) : ClsResolver {

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
      return ClassFileOrigin.PluginInternalClass
    }
    if (jdkClassesResolver.containsClass(className)) {
      return ClassFileOrigin.JdkClass
    }
    if (ideResolver.containsClass(className)) {
      return ClassFileOrigin.IdeClass
    }
    if (dependenciesResolver.containsClass(className)) {
      return ClassFileOrigin.ClassOfPluginDependency
    }
    return null
  }

}