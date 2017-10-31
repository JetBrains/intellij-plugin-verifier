package com.jetbrains.pluginverifier.verifiers

import com.jetbrains.pluginverifier.parameters.ide.IdeDescriptor
import com.jetbrains.pluginverifier.results.location.Location
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

class IdeClassesVisitor {

  @Suppress("UNCHECKED_CAST")
      //todo: use concurrency
  fun detectIdeDeprecatedApiElements(ideDescriptor: IdeDescriptor): Set<Location> {
    val ideClassLoader = ideDescriptor.ideResolver
    val deprecatedElements = hashSetOf<Location>()

    for (className in ideClassLoader.allClasses) {
      val classNode = ideClassLoader.findClass(className)
      if (classNode != null) {
        val methods = classNode.methods as List<MethodNode>
        val fields = classNode.fields as List<FieldNode>

        if (classNode.isDeprecated()) {
          deprecatedElements.add(classLocationByClassNode(classNode, ideClassLoader))
        }

        methods
            .asSequence()
            .filter { it -> it.isDeprecated() }
            .mapTo(deprecatedElements) { methodLocationByClassAndMethodNodes(classNode, it, ideClassLoader) }

        fields
            .asSequence()
            .filter { it.isDeprecated() }
            .mapTo(deprecatedElements) { fieldLocationByFieldAndClassNodes(classNode, it, ideClassLoader) }
      }
    }

    return deprecatedElements
  }

}