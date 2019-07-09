package com.jetbrains.pluginverifier.tasks.deprecatedUsages

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.usages.deprecated.getDeprecationInfo
import com.jetbrains.pluginverifier.verifiers.resolution.resolveClassOrNull
import java.util.concurrent.Callable
import java.util.concurrent.ForkJoinPool
import java.util.stream.Collectors

class DeprecatedIdeClassesVisitor {

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

    val classFile = resolveClassOrNull(className) ?: return emptySet()

    if (classFile.getDeprecationInfo() != null) {
      deprecatedElements += classFile.location
    }

    classFile.methods
        .asSequence()
        .filter { it.isDeprecated }
        .mapTo(deprecatedElements) { it.location }

    classFile.fields
        .asSequence()
        .filter { it.isDeprecated }
        .mapTo(deprecatedElements) { it.location }

    return deprecatedElements
  }

}