package com.jetbrains.pluginverifier.verifiers

import com.jetbrains.plugin.structure.base.utils.rethrowIfInterrupted
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.IntelliJClassFileOrigin
import java.util.concurrent.Callable
import java.util.concurrent.ForkJoinPool
import java.util.stream.Collectors

class IdeClassesVisitor {

  fun detectIdeDeprecatedApiElements(ideDescriptor: IdeDescriptor): Set<Location> {
    val ideClassLoader = ideDescriptor.ideResolver
    val allClasses = ideClassLoader.allClasses
    val forkJoinPool = ForkJoinPool(maxOf(4, Runtime.getRuntime().availableProcessors()))
    return forkJoinPool.submit(Callable {
      allClasses.parallelStream()
          .flatMap { ideClassLoader.findDeprecatedApiOfClass(it).stream() }
          .collect(Collectors.toSet())
    }).get()
  }

  private fun Resolver.findDeprecatedApiOfClass(className: String): Set<Location> {
    val deprecatedElements = hashSetOf<Location>()

    val classNode = try {
      findClass(className)
    } catch (e: Exception) {
      e.rethrowIfInterrupted()
      null
    }

    val classPath = getClassLocation(className)?.classPath?.firstOrNull()?.toPath()

    if (classNode != null && classPath != null) {
      val classFile = ClassFile(classNode, IntelliJClassFileOrigin.IdeClass(classPath))

      if (classFile.getDeprecationInfo() != null) {
        deprecatedElements.add(classFile.location)
      }

      classFile.methods
          .asSequence()
          .filter { it.isDeprecated }
          .mapTo(deprecatedElements) { it.location }

      classFile.fields
          .asSequence()
          .filter { it.isDeprecated }
          .mapTo(deprecatedElements) { it.location }
    }

    return deprecatedElements
  }

}