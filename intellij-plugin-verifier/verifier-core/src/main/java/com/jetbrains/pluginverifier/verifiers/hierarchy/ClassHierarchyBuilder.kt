package com.jetbrains.pluginverifier.verifiers.hierarchy

import com.jetbrains.pluginverifier.results.hierarchy.ClassHierarchy
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.resolveClassChecked

class ClassHierarchyBuilder(private val context: VerificationContext) {

  fun buildClassHierarchy(classFile: ClassFile): ClassHierarchy {
    val className2Hierarchy = hashMapOf<String, ClassHierarchy>()

    val parentsVisitor = ClassParentsVisitor(true) { subclassFile, superName ->
      context.classResolver.resolveClassChecked(superName, subclassFile, context)
    }
    parentsVisitor.visitClass(
        classFile,
        true,
        onEnter = { parent ->
          className2Hierarchy[parent.name] = ClassHierarchy(
              parent.name,
              parent.isInterface,
              null,
              emptyList()
          )
          true
        },
        onExit = { parent ->
          val classHierarchy = className2Hierarchy[parent.name]!!

          val superName = parent.superName
          if (superName != null) {
            classHierarchy.superClass = className2Hierarchy[superName]
          }

          classHierarchy.superInterfaces = parent.interfaces.mapNotNull { className2Hierarchy[it] }
        }
    )

    return className2Hierarchy[classFile.name]!!
  }


}