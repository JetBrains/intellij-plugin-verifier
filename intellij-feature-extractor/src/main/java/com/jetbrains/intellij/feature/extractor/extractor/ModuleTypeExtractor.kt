package com.jetbrains.intellij.feature.extractor.extractor

import com.jetbrains.intellij.feature.extractor.*
import com.jetbrains.plugin.structure.classes.resolvers.ResolutionResult
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.VarInsnNode
import org.objectweb.asm.tree.analysis.SourceValue

class ModuleTypeExtractor : Extractor {

  companion object {
    private const val MODULE_TYPE_CLASS_NAME = "com/intellij/openapi/module/ModuleType"
  }

  override fun extract(plugin: IdePlugin, resolver: Resolver): List<ExtensionPointFeatures> {
    return getExtensionPointImplementors(plugin, resolver, ExtensionPoint.MODULE_TYPE)
        .mapNotNull { extractModuleType(it, resolver) }
  }

  private fun extractModuleType(classNode: ClassNode, resolver: Resolver): ExtensionPointFeatures? {
    if (classNode.superName == MODULE_TYPE_CLASS_NAME) {
      return convertResult(extractFromClassNode(classNode, resolver))
    }
    val constructors = classNode.findMethods { it.name == "<init>" }
    for (constructor in constructors) {
      if (constructor.desc == "()V") {
        val isDefaultParentInvocation = constructor.instructionsAsList().any {
          it is MethodInsnNode
              && it.opcode == Opcodes.INVOKESPECIAL
              && it.owner == classNode.superName
              && it.desc == "()V"
        }
        if (isDefaultParentInvocation) {
          val superNode = (resolver.resolveClass(classNode.superName) as? ResolutionResult.Found)?.classNode ?: continue
          return convertResult(extractFromClassNode(superNode, resolver))
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

  private fun extractFromClassNode(classNode: ClassNode, resolver: Resolver): String? {
    val constructors = classNode.findMethods { it.name == "<init>" }
    for (constructor in constructors) {
      val instructionsAsList = constructor.instructionsAsList()
      val superClassConstructorInitIndex = instructionsAsList.indexOfLast { instructionNode ->
        instructionNode is MethodInsnNode
            && instructionNode.name == "<init>"
            && instructionNode.desc == "(Ljava/lang/String;)V"
            && instructionNode.opcode == Opcodes.INVOKESPECIAL
            && instructionNode.owner == MODULE_TYPE_CLASS_NAME
      }
      if (superClassConstructorInitIndex != -1) {
        val constructorFrames = AnalysisUtil.analyzeMethodFrames(classNode, constructor)
        val moduleIdArgumentValue = constructorFrames[superClassConstructorInitIndex].getOnStack(0)
        val moduleIdPassedToSuperClass = AnalysisUtil.evaluateConstantString(
            moduleIdArgumentValue,
            resolver,
            constructorFrames,
            instructionsAsList
        )
        if (moduleIdPassedToSuperClass != null) {
          return moduleIdPassedToSuperClass
        }

        if (moduleIdArgumentValue is SourceValue && moduleIdArgumentValue.insns.size == 1) {
          val moduleIdProducerInstruction = moduleIdArgumentValue.insns.first()
          if (moduleIdProducerInstruction is VarInsnNode) {
            val moduleIdParameterIndex = moduleIdProducerInstruction.`var`

            val moduleIdCalleeStackIndex = AnalysisUtil.getMethodParametersNumber(constructor) - moduleIdParameterIndex

            val passedModuleId = findDelegatingConstructorAndExtractPassedValue(
                constructor,
                constructors,
                classNode,
                moduleIdCalleeStackIndex,
                resolver
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
      thisConstructor: MethodNode,
      allConstructors: List<MethodNode>,
      classNode: ClassNode,
      moduleIdCalleeStackIndex: Int,
      resolver: Resolver
  ): String? {
    for (otherConstructor in allConstructors) {
      val otherConstructorInstruction = otherConstructor.instructionsAsList()

      val thisConstructorCallIndex = otherConstructorInstruction.indexOfLast {
        it is MethodInsnNode
            && it.name == "<init>"
            && it.desc == thisConstructor.desc
            && it.opcode == Opcodes.INVOKESPECIAL
            && it.owner == classNode.name
      }

      if (thisConstructorCallIndex != -1) {
        val frames = AnalysisUtil.analyzeMethodFrames(classNode, otherConstructor)
        val thisConstructorInvocationFrame = frames[thisConstructorCallIndex]
        if (moduleIdCalleeStackIndex < thisConstructorInvocationFrame.stackSize) {
          val delegatedValue = thisConstructorInvocationFrame.getOnStack(moduleIdCalleeStackIndex)
          return AnalysisUtil.evaluateConstantString(delegatedValue, resolver, frames, otherConstructorInstruction)
        }
      }
    }
    return null
  }

}