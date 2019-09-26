package com.jetbrains.intellij.feature.extractor.extractor

import com.jetbrains.intellij.feature.extractor.ExtensionPoint
import com.jetbrains.intellij.feature.extractor.ExtensionPointFeatures
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.pluginverifier.verifiers.analyzeMethodFrames
import com.jetbrains.pluginverifier.verifiers.evaluateConstantString
import com.jetbrains.pluginverifier.verifiers.extractConstantFunctionValue
import com.jetbrains.pluginverifier.verifiers.getOnStack
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import org.objectweb.asm.Opcodes
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

  private fun extractConfigurationTypes(classNode: ClassFile, resolver: Resolver): ExtensionPointFeatures? {
    if (classNode.superName == CONFIGURATION_BASE) {
      val constructor = classNode.methods.find { it.isConstructor } ?: return null
      val frames = analyzeMethodFrames(constructor)
      val constructorInstructions = constructor.instructions
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
      val value = evaluateConstantString(frames[superInitIndex].getOnStack(3), resolver, frames.toList(), constructorInstructions)
      return convertToResult(value)
    } else {
      val method = classNode.methods.find { it.name == "getId" && it.methodParameters.isEmpty() } ?: return null
      if (method.isAbstract) {
        return null
      }
      val value = extractConstantFunctionValue(method, resolver)
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