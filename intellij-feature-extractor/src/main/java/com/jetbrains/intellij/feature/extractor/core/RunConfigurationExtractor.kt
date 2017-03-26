package com.jetbrains.intellij.feature.extractor.core

import com.intellij.structure.resolvers.Resolver
import org.jetbrains.intellij.plugins.internal.asm.Type
import org.jetbrains.intellij.plugins.internal.asm.tree.ClassNode
import org.jetbrains.intellij.plugins.internal.asm.tree.MethodInsnNode
import org.jetbrains.intellij.plugins.internal.asm.tree.analysis.Analyzer
import org.jetbrains.intellij.plugins.internal.asm.tree.analysis.SourceInterpreter

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
      val init = classNode.findMethod({ it.name == "<init>" }) ?: return null
      val frames = Analyzer(SourceInterpreter()).analyze(classNode.name, init)
      val superInitIndex = init.instructions.toArray().indexOfLast {
        it is MethodInsnNode
            && it.name == "<init>"
            && it.owner == CONFIGURATION_BASE
            && it.desc == "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljavax/swing/Icon;)V"
      }
      if (superInitIndex == -1) {
        return null
      }
      val value = AnalysisUtil.evaluateConstantString(frames[superInitIndex].getOnStack(3), resolver, frames.toList(), init.instructionsAsList())
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