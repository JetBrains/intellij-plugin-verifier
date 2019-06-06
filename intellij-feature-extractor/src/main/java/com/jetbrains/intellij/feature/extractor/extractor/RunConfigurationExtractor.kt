package com.jetbrains.intellij.feature.extractor.extractor

import com.jetbrains.intellij.feature.extractor.*
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
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
class RunConfigurationExtractor : Extractor {

  companion object {
    private const val CONFIGURATION_BASE = "com/intellij/execution/configurations/ConfigurationTypeBase"
  }

  override fun extract(plugin: IdePlugin, resolver: Resolver): List<ExtensionPointFeatures> {
    return getExtensionPointImplementors(plugin, resolver, ExtensionPoint.CONFIGURATION_TYPE)
        .mapNotNull { extractConfigurationTypes(it, resolver) }
  }

  private fun extractConfigurationTypes(classNode: ClassNode, resolver: Resolver): ExtensionPointFeatures? {
    if (classNode.superName == CONFIGURATION_BASE) {
      val constructor = classNode.findMethod { it.isConstructor } ?: return null
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
      return convertToResult(value)
    } else {
      val method = classNode.findMethod { it.name == "getId" && Type.getArgumentTypes(it.desc).isEmpty() }
          ?: return null
      if (method.isAbstract()) {
        return null
      }
      val value = AnalysisUtil.extractConstantFunctionValue(classNode, method, resolver)
      return convertToResult(value)
    }
  }

  private fun convertToResult(value: String?): ExtensionPointFeatures? =
      if (value != null) {
        ExtensionPointFeatures(ExtensionPoint.CONFIGURATION_TYPE, listOf(value))
      } else {
        null
      }
}