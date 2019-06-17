package com.jetbrains.pluginverifier.verifiers.resolution

import com.jetbrains.plugin.structure.base.utils.closeLogged
import com.jetbrains.plugin.structure.base.utils.rethrowIfInterrupted
import com.jetbrains.plugin.structure.classes.resolvers.CacheResolver
import com.jetbrains.plugin.structure.classes.resolvers.InvalidClassFileException
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.pluginverifier.parameters.packages.PackageFilter
import java.io.Closeable

/**
 * [ClassResolver] for verification of a plugin against API of another plugin.
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
class PluginApiClassResolver(
    checkedPluginResolver: Resolver,
    basePluginResolver: Resolver,
    jdkClassesResolver: Resolver,
    private val closeableResources: List<Closeable>,
    private val basePluginPackageFilter: PackageFilter
) : IntelliJClassResolver() {

  private val cachedCheckedPluginResolver = CacheResolver(checkedPluginResolver)
  private val cachedBasePluginResolver = CacheResolver(basePluginResolver)
  private val cachedJdkClassesResolver = CacheResolver(jdkClassesResolver)
  private val allResolvers = listOf(cachedCheckedPluginResolver, cachedBasePluginResolver, cachedJdkClassesResolver)

  private fun isExternalClass(className: String) = !basePluginPackageFilter.accept(className) && allResolvers.none { it.containsClass(className) }

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
        val classPath = resolver.getClassLocation(className)?.classPath?.firstOrNull()
        if (classPath != null) {
          val classFileOrigin = when (resolver) {
            cachedCheckedPluginResolver -> IntelliJClassFileOrigin.PluginClass(classPath)
            cachedJdkClassesResolver -> IntelliJClassFileOrigin.JdkClass(classPath)
            cachedBasePluginResolver -> IntelliJClassFileOrigin.ClassOfPluginDependency(classPath)
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