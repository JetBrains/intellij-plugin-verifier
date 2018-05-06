package com.jetbrains.pluginverifier.verifiers.logic.hierarchy

import com.jetbrains.pluginverifier.results.hierarchy.ClassHierarchy
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.isInterface
import com.jetbrains.pluginverifier.verifiers.logic.CommonClassNames
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileOrigin
import org.objectweb.asm.tree.ClassNode

class ClassHierarchyBuilder(private val context: VerificationContext) {

  companion object {
    val JAVA_LANG_OBJECT_HIERARCHY = ClassHierarchy(
        CommonClassNames.JAVA_LANG_OBJECT,
        false,
        ClassFileOrigin.JDK_CLASS,
        null,
        emptyList()
    )
  }

  fun buildClassHierarchy(classNode: ClassNode): ClassHierarchy {
    if (classNode.name == CommonClassNames.JAVA_LANG_OBJECT) {
      return JAVA_LANG_OBJECT_HIERARCHY
    }

    val className2Hierarchy = hashMapOf<String, ClassHierarchy>()

    ClassParentsVisitor(context, true).visitClass(classNode, true,
        onEnter = { parent ->
          val parentOrigin = context.clsResolver.getOriginOfClass(parent.name)
          if (parentOrigin != null) {
            className2Hierarchy[parent.name] = ClassHierarchy(
                parent.name,
                parent.isInterface(),
                parentOrigin,
                null,
                emptyList()
            )
          }
          true
        },
        onExit = { parent ->
          val classHierarchy = className2Hierarchy[parent.name]!!

          val superName = parent.superName
          if (superName != null) {
            classHierarchy.superClass = className2Hierarchy[superName]
          }

          @Suppress("UNCHECKED_CAST")
          val superInterfacesNames = parent.interfaces as List<String>
          classHierarchy.superInterfaces = superInterfacesNames.mapNotNull { className2Hierarchy[it] }
        }
    )

    return className2Hierarchy[classNode.name]!!
  }


}