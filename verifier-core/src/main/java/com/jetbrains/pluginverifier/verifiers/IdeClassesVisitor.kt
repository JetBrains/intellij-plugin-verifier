package com.jetbrains.pluginverifier.verifiers

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.misc.checkIfInterrupted
import com.jetbrains.pluginverifier.results.location.Location
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
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

  @Suppress("UNCHECKED_CAST")
  private fun Resolver.findDeprecatedApiOfClass(className: String): Set<Location> {
    val deprecatedElements = hashSetOf<Location>()

    val classNode = try {
      findClass(className)
    } catch (ie: InterruptedException) {
      throw ie
    } catch (e: Exception) {
      checkIfInterrupted()
      null
    }
    if (classNode != null) {
      val methods = classNode.methods as List<MethodNode>
      val fields = classNode.fields as List<FieldNode>

      if (classNode.isDeprecated()) {
        deprecatedElements.add(classNode.createClassLocation())
      }

      methods
          .asSequence()
          .filter { it -> it.isDeprecated() }
          .mapTo(deprecatedElements) { createMethodLocation(classNode, it) }

      fields
          .asSequence()
          .filter { it.isDeprecated() }
          .mapTo(deprecatedElements) { createFieldLocation(classNode, it) }
    }

    return deprecatedElements
  }

}