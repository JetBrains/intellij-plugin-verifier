package com.jetbrains.pluginverifier.verifiers.resolution

import com.jetbrains.plugin.structure.base.utils.closeLogged
import com.jetbrains.plugin.structure.base.utils.rethrowIfInterrupted
import com.jetbrains.plugin.structure.classes.resolvers.CacheResolver
import com.jetbrains.plugin.structure.classes.resolvers.InvalidClassFileException
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.pluginverifier.parameters.packages.PackageFilter
import java.io.Closeable

/**
 * [ClassResolver] for verification of an IntelliJ plugin against an IDE.
 * Resolves classes in the following order:
 * 1) Plugin
 * 2) JDK classes
 * 3) IDE classes
 * 4) Plugin's dependencies
 */
class IdePluginClassResolver(
    pluginResolver: Resolver,
    dependenciesResolver: Resolver,
    jdkClassesResolver: Resolver,
    ideResolver: Resolver,
    private val externalClassesPackageFilter: PackageFilter,
    private val closeableResources: List<Closeable>
) : IntelliJClassResolver() {

  private val cachedPluginResolver = CacheResolver(pluginResolver)
  private val cachedJdkClassesResolver = CacheResolver(jdkClassesResolver)
  private val cachedIdeResolver = CacheResolver(ideResolver)
  private val cachedDependenciesResolver = CacheResolver(dependenciesResolver)
  private val allResolvers = listOf(cachedPluginResolver, cachedJdkClassesResolver, cachedIdeResolver, cachedDependenciesResolver)

  private fun isExternalClass(className: String) = externalClassesPackageFilter.accept(className)

  override fun packageExists(packageName: String) = allResolvers.any { it.containsPackage(packageName) }

  override fun doResolveClass(className: String): ClassResolutionResult {
    if (isExternalClass(className)) {
      return ClassResolutionResult.ExternalClass
    }
    for (resolver in allResolvers) {
      val classNode = try {
        resolver.findClass(className)
      } catch (e: InvalidClassFileException) {
        return ClassResolutionResult.InvalidClassFile(e.message)
      } catch (e: Exception) {
        e.rethrowIfInterrupted()
        return ClassResolutionResult.FailedToReadClassFile(e.message ?: e.javaClass.name)
      }

      if (classNode != null) {
        val classPath = resolver.getClassLocation(className)?.classPath?.firstOrNull()?.toPath()
        if (classPath != null) {
          val classFileOrigin = when (resolver) {
            cachedPluginResolver -> IntelliJClassFileOrigin.PluginClass(classPath)
            cachedJdkClassesResolver -> IntelliJClassFileOrigin.JdkClass(classPath)
            cachedIdeResolver -> IntelliJClassFileOrigin.IdeClass(classPath)
            cachedDependenciesResolver -> IntelliJClassFileOrigin.ClassOfPluginDependency(classPath)
            else -> error("")
          }
          val classFile = ClassFileAsm(classNode, classFileOrigin)
          return ClassResolutionResult.Found(classFile)
        }
      }
    }
    return ClassResolutionResult.NotFound
  }

  override fun close() {
    closeableResources.forEach { it.closeLogged() }
  }
}