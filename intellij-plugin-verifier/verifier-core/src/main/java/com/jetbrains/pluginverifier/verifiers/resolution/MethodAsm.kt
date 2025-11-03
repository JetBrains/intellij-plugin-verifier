/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.verifiers.resolution

import com.jetbrains.plugin.structure.base.utils.toList
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.modifiers.Modifiers
import com.jetbrains.pluginverifier.verifiers.getAccessType
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.LocalVariableNode
import org.objectweb.asm.tree.MethodNode

class MethodAsm(override val containingClassFile: ClassFile, val asmNode: MethodNode) : Method {
  override val location
    get() = MethodLocation(
      containingClassFile.location,
      name,
      descriptor,
      methodParameters.map { it.name },
      signature?.takeIf { it.isNotEmpty() },
      Modifiers(asmNode.access)
    )

  override val name: String
    get() = asmNode.name

  override val descriptor: String
    get() = asmNode.desc

  override val accessType
    get() = getAccessType(asmNode.access)

  override val signature: String?
    get() = asmNode.signature

  override val annotations: List<AnnotationNode>
    get() = asmNode.invisibleAnnotations.orEmpty() + asmNode.visibleAnnotations.orEmpty()

  override val localVariables: List<LocalVariableNode>
    get() = asmNode.localVariables.orEmpty()

  override val methodParameters: List<MethodParameter>
    get() {
      val parameterNames = asmNode.getParameterNames()
      val parameterAnnotations: Array<out MutableList<AnnotationNode>?> = asmNode.invisibleParameterAnnotations.orEmpty()

      //The first parameter is a parameter of an inner class' constructor => ignore the first annotation.
      if (name == "<init>" && containingClassFile.isInnerClass) {
        return parameterNames.mapIndexed { index, parameterName ->
          MethodParameter(parameterName, parameterAnnotations.getOrElse(index - 1) { emptyList<AnnotationNode>() }.orEmpty())
        }
      }

      //The simplest case: just zip parameter names and annotations.
      if (parameterNames.size == parameterAnnotations.size) {
        return parameterNames.mapIndexed { index, parameterName ->
          MethodParameter(parameterName, parameterAnnotations[index].orEmpty())
        }
      }

      //Fallback: we don't know how to zip parameter names and annotations.
      return parameterNames.map { parameterName -> MethodParameter(parameterName, emptyList()) }
    }

  override val exceptions
    get() = asmNode.exceptions.orEmpty()

  override val tryCatchBlocks
    get() = asmNode.tryCatchBlocks.orEmpty()

  override val instructions: List<AbstractInsnNode>
    get() = asmNode.instructions.iterator().toList()


  override val isAbstract
    get() = asmNode.access and Opcodes.ACC_ABSTRACT != 0

  override val isStatic
    get() = asmNode.access and Opcodes.ACC_STATIC != 0

  override val isFinal
    get() = asmNode.access and Opcodes.ACC_FINAL != 0

  override val isPublic
    get() = asmNode.access and Opcodes.ACC_PUBLIC != 0

  override val isProtected
    get() = asmNode.access and Opcodes.ACC_PROTECTED != 0

  override val isPrivate
    get() = asmNode.access and Opcodes.ACC_PRIVATE != 0

  override val isPackagePrivate
    get() = (asmNode.access and Opcodes.ACC_PUBLIC == 0) && (asmNode.access and Opcodes.ACC_PROTECTED == 0) && (asmNode.access and Opcodes.ACC_PRIVATE == 0)

  override val isDeprecated
    get() = asmNode.access and Opcodes.ACC_DEPRECATED != 0

  override val isVararg
    get() = asmNode.access and Opcodes.ACC_VARARGS != 0

  override val isConstructor: Boolean
    get() = asmNode.name == "<init>"

  override val isClassInitializer: Boolean
    get() = asmNode.name == "<clinit>"

  override val isNative
    get() = asmNode.access and Opcodes.ACC_NATIVE != 0

  override val isSynthetic
    get() = asmNode.access and Opcodes.ACC_SYNTHETIC != 0

  override val isBridgeMethod
    get() = asmNode.access and Opcodes.ACC_BRIDGE != 0

  private fun MethodNode.getParameterNames(): List<String> {
    val descriptorArguments = Type.getArgumentTypes(desc)
    val descriptorArgumentsNumber = descriptorArguments.size

    if (localVariables != null) {
      val allLocalVars = localVariables.sortedBy { it.index }
      val parameters = if (access and Opcodes.ACC_STATIC != 0) {
        allLocalVars.take(descriptorArgumentsNumber)
      } else {
        allLocalVars.drop(1).take(descriptorArgumentsNumber)
      }

      if (parameters.size == descriptorArgumentsNumber
        && parameters.indices.all { index -> parameters[index].desc == descriptorArguments[index]?.descriptor }
      ) {
        return parameters.map { it.name }
      }
    }

    return (0 until descriptorArgumentsNumber).map { "arg$it" }
  }

}