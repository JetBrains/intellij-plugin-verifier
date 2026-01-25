/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.intellij.feature.extractor.extractor

import com.jetbrains.intellij.feature.extractor.ExtensionPoint
import com.jetbrains.intellij.feature.extractor.ExtensionPointFeatures
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.pluginverifier.verifiers.CodeAnalysis
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.MethodInsnNode

/**
 * Extracts value returned by ArtifactType#getId() from a class extending ArtifactType.
 */
class ArtifactTypeExtractor : Extractor {

  override fun extract(plugin: IdePlugin, resolver: Resolver) =
    getExtensionPointImplementors(plugin, resolver, ExtensionPoint.ARTIFACT_TYPE)
      .flatMap { extractArtifactTypes(it) }

  private fun extractArtifactTypes(classFile: ClassFile): List<ExtensionPointFeatures> {
    return classFile.methods.filter { it.isConstructor }
      .mapNotNull { extractArtifactTypesFromConstructor(it, classFile) }
      .toList()
  }

  private fun extractArtifactTypesFromConstructor(
    classConstructor: Method,
    classFile: ClassFile
  ): ExtensionPointFeatures? {
    val instructions = classConstructor.instructions
    val superInitIndex = instructions.indexOfLast {
      it is MethodInsnNode && it.opcode == Opcodes.INVOKESPECIAL && it.name == "<init>" && it.owner == classFile.superName
    }
    if (superInitIndex == -1) {
      return null
    }
    val superInitDesc = (instructions[superInitIndex] as? MethodInsnNode)?.desc ?: return null
    val argumentsNumber = Type.getArgumentCount(superInitDesc)

    val stringValue = CodeAnalysis().evaluateConstantString(classConstructor, superInitIndex, argumentsNumber - 1)
    if (stringValue != null) {
      return ExtensionPointFeatures(ExtensionPoint.ARTIFACT_TYPE, listOf(stringValue))
    }
    return null
  }

}