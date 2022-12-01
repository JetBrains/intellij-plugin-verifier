/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.intellij.feature.extractor.extractor

import com.jetbrains.intellij.feature.extractor.ExtensionPoint
import com.jetbrains.intellij.feature.extractor.ExtensionPointFeatures
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.pluginverifier.verifiers.CodeAnalysis
import com.jetbrains.pluginverifier.verifiers.analyzeMethodFrames
import com.jetbrains.pluginverifier.verifiers.getOnStack
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import com.jetbrains.pluginverifier.verifiers.resolution.resolveClassOrNull
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.MethodInsnNode
import org.jetbrains.org.objectweb.asm.tree.VarInsnNode

class ModuleTypeExtractor : Extractor {

  companion object {
    private const val MODULE_TYPE_CLASS_NAME = "com/intellij/openapi/module/ModuleType"
  }

  override fun extract(plugin: IdePlugin, resolver: Resolver): List<ExtensionPointFeatures> {
    return getExtensionPointImplementors(plugin, resolver, ExtensionPoint.MODULE_TYPE)
      .mapNotNull { extractModuleType(it, resolver) }
  }

  private fun extractModuleType(classFile: ClassFile, resolver: Resolver): ExtensionPointFeatures? {
    if (classFile.superName == MODULE_TYPE_CLASS_NAME) {
      return convertResult(extractFromClassNode(classFile))
    }
    val constructors = classFile.methods.filter { it.isConstructor }
    for (constructor in constructors) {
      if (constructor.descriptor == "()V") {
        val isDefaultParentInvocation = constructor.instructions.any {
          it is MethodInsnNode
            && it.opcode == Opcodes.INVOKESPECIAL
            && it.owner == classFile.superName
            && it.desc == "()V"
        }
        val superName = classFile.superName
        if (isDefaultParentInvocation && superName != null) {
          val superClassFile = resolver.resolveClassOrNull(superName) ?: continue
          return convertResult(extractFromClassNode(superClassFile))
        }
      }
    }
    return null
  }

  private fun convertResult(moduleId: String?): ExtensionPointFeatures? {
    if (moduleId != null) {
      return ExtensionPointFeatures(ExtensionPoint.MODULE_TYPE, listOf(moduleId))
    }
    return null
  }

  private fun extractFromClassNode(classFile: ClassFile): String? {
    val constructors = classFile.methods.filter { it.isConstructor }
    for (constructor in constructors) {
      val instructionsAsList = constructor.instructions
      val superClassConstructorInitIndex = instructionsAsList.indexOfLast { instructionNode ->
        instructionNode is MethodInsnNode
          && instructionNode.name == "<init>"
          && instructionNode.desc == "(Ljava/lang/String;)V"
          && instructionNode.opcode == Opcodes.INVOKESPECIAL
          && instructionNode.owner == MODULE_TYPE_CLASS_NAME
      }
      if (superClassConstructorInitIndex != -1) {
        val constructorFrames = analyzeMethodFrames(constructor) ?: return null
        val moduleIdArgumentValue = constructorFrames[superClassConstructorInitIndex].getOnStack(0) ?: return null
        val moduleIdPassedToSuperClass = CodeAnalysis().evaluateConstantString(
          constructor,
          constructorFrames,
          moduleIdArgumentValue
        )
        if (moduleIdPassedToSuperClass != null) {
          return moduleIdPassedToSuperClass
        }

        if (moduleIdArgumentValue.insns.size == 1) {
          val moduleIdProducerInstruction = moduleIdArgumentValue.insns.first()
          if (moduleIdProducerInstruction is VarInsnNode) {
            val moduleIdParameterIndex = moduleIdProducerInstruction.`var`

            val moduleIdCalleeStackIndex = constructor.methodParameters.size - moduleIdParameterIndex

            val passedModuleId = findDelegatingConstructorAndExtractPassedValue(
              constructor,
              constructors,
              classFile,
              moduleIdCalleeStackIndex
            )
            if (passedModuleId != null) {
              return passedModuleId
            }
          }
        }
      }
    }
    return null
  }

  private fun findDelegatingConstructorAndExtractPassedValue(
    thisConstructor: Method,
    allConstructors: Sequence<Method>,
    classNode: ClassFile,
    moduleIdCalleeStackIndex: Int
  ): String? {
    for (otherConstructor in allConstructors) {
      val otherConstructorInstruction = otherConstructor.instructions

      val thisConstructorCallIndex = otherConstructorInstruction.indexOfLast {
        it is MethodInsnNode
          && it.name == "<init>"
          && it.desc == thisConstructor.descriptor
          && it.opcode == Opcodes.INVOKESPECIAL
          && it.owner == classNode.name
      }

      if (thisConstructorCallIndex != -1) {
        return CodeAnalysis().evaluateConstantString(otherConstructor, thisConstructorCallIndex, moduleIdCalleeStackIndex)
      }
    }
    return null
  }

}