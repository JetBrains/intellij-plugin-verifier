/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.intellij.feature.extractor.extractor

import com.jetbrains.intellij.feature.extractor.ExtensionPoint
import com.jetbrains.intellij.feature.extractor.ExtensionPointFeatures
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.pluginverifier.verifiers.CodeAnalysis
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
      .mapNotNull { extractConfigurationTypes(it) }
  }

  private fun extractConfigurationTypes(classNode: ClassFile): ExtensionPointFeatures? {
    if (classNode.superName == CONFIGURATION_BASE) {
      val constructor = classNode.methods.find { it.isConstructor } ?: return null
      val superInitIndex = constructor.instructions.indexOfLast {
        it is MethodInsnNode
          && it.name == "<init>"
          && it.opcode == Opcodes.INVOKESPECIAL
          && it.owner == CONFIGURATION_BASE
          && it.desc == "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljavax/swing/Icon;)V"
      }
      if (superInitIndex == -1) {
        return null
      }
      val value = CodeAnalysis().evaluateConstantString(constructor, superInitIndex, 3)
      return convertToResult(value)
    } else {
      val method = classNode.methods.find { it.name == "getId" && it.methodParameters.isEmpty() } ?: return null
      if (method.isAbstract) {
        return null
      }
      val value = CodeAnalysis().evaluateConstantFunctionValue(method)
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