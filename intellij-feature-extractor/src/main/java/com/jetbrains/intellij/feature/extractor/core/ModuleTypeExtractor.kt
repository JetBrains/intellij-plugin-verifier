package com.jetbrains.intellij.feature.extractor.core

import com.intellij.structure.resolvers.Resolver
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.VarInsnNode
import org.objectweb.asm.tree.analysis.SourceValue

/**
 * @author Sergey Patrikeev
 */
class ModuleTypeExtractor(resolver: Resolver) : Extractor(resolver) {

  private val MODULE_TYPE_CLASS_NAME = "com/intellij/openapi/module/ModuleType"

  override fun extractImpl(classNode: ClassNode): List<String>? {
    if (classNode.superName == MODULE_TYPE_CLASS_NAME) {
      return convertResult(extractFromClassNode(classNode))
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
          val superNode = resolver.findClass(classNode.superName) ?: continue
          return convertResult(extractFromClassNode(superNode))
        }
      }
    }
    return null
  }

  private fun convertResult(moduleId: String?): List<String>? {
    if (moduleId != null) {
      extractedAll = true
      return listOf(moduleId)
    }
    return moduleId
  }

  private fun extractFromClassNode(classNode: ClassNode): String? {
    val constructors = classNode.findMethods { it.name == "<init>" }
    for (constructor in constructors) {
      val instructionsAsList = constructor.instructionsAsList()
      val superClassConstructorInitIndex = instructionsAsList.indexOfLast { insnNode ->
        insnNode is MethodInsnNode
            && insnNode.name == "<init>"
            && insnNode.desc == "(Ljava/lang/String;)V"
            && insnNode.opcode == Opcodes.INVOKESPECIAL
            && insnNode.owner == MODULE_TYPE_CLASS_NAME
      }
      if (superClassConstructorInitIndex != -1) {
        val constructorFrames = AnalysisUtil.analyzeMethodFrames(classNode, constructor)
        val moduleIdArgumentValue = constructorFrames[superClassConstructorInitIndex].getOnStack(0)
        val moduleIdPassedToSuperClass = AnalysisUtil.evaluateConstantString(moduleIdArgumentValue, resolver, constructorFrames, instructionsAsList)
        if (moduleIdPassedToSuperClass != null) {
          return moduleIdPassedToSuperClass
        }

        if (moduleIdArgumentValue is SourceValue && moduleIdArgumentValue.insns.size == 1) {
          val moduleIdProducerInsn = moduleIdArgumentValue.insns.first()
          if (moduleIdProducerInsn is VarInsnNode) {
            val moduleIdParameterIndex = moduleIdProducerInsn.`var`

            val moduleIdCalleeStackIndex = AnalysisUtil.getMethodParametersNumber(constructor) - moduleIdParameterIndex

            val passedModuleId = findDelegatingConstructorAndExtractPassedValue(constructor, constructors, classNode, moduleIdCalleeStackIndex)
            if (passedModuleId != null) {
              return passedModuleId
            }
          }
        }
      }
    }
    return null
  }

  private fun findDelegatingConstructorAndExtractPassedValue(thisConstructor: MethodNode,
                                                             allConstructors: List<MethodNode>,
                                                             classNode: ClassNode,
                                                             moduleIdCalleeStackIndex: Int): String? {
    for (otherConstructor in allConstructors) {
      val otherConstructorInsn = otherConstructor.instructionsAsList()

      val thisConstructorCallIndex = otherConstructorInsn.indexOfLast {
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
          return AnalysisUtil.evaluateConstantString(delegatedValue, resolver, frames, otherConstructorInsn)
        }
      }
    }
    return null
  }

}