package com.jetbrains.intellij.feature.extractor.core

import com.intellij.structure.resolvers.Resolver
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode

/**
 * Extracts value returned by ConfigurationType#getId from a class extending ConfigurationType.
 *
 * Extracts id of the run configuration
 * (as if com.intellij.execution.configurations.ConfigurationType.getId() is invoked)
 */
class RunConfigurationExtractor(resolver: Resolver) : Extractor(resolver) {

  private val CONFIGURATION_BASE = "com/intellij/execution/configurations/ConfigurationTypeBase"

  override fun extractImpl(classNode: ClassNode): List<String>? {
    if (classNode.superName == CONFIGURATION_BASE) {
      val constructor = classNode.findMethod { it.name == "<init>" } ?: return null
      val frames = AnalysisUtil.analyzeMethodFrames(classNode, constructor)
      val constructorInstructions = constructor.instructionsAsList()
      val superInitIndex = constructorInstructions.indexOfLast {
        it is MethodInsnNode
            && it.name == "<init>"
            && it.opcode == Opcodes.INVOKESPECIAL
            && it.owner == CONFIGURATION_BASE
            && it.desc == "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljavax/swing/Icon;)V"
      }
      if (superInitIndex == -1) {
        return null
      }
      val value = AnalysisUtil.evaluateConstantString(frames[superInitIndex].getOnStack(3), resolver, frames.toList(), constructorInstructions)
      if (value != null) {
        extractedAll = true
        return listOf(value)
      }
      return null
    } else {
      val method = classNode.findMethod({ it.name == "getId" && Type.getArgumentTypes(it.desc).isEmpty() }) ?: return null
      if (method.isAbstract()) {
        return null
      }
      val value = AnalysisUtil.extractConstantFunctionValue(classNode, method, resolver)
      return if (value == null) null else {
        extractedAll = true
        listOf(value)
      }
    }
  }
}