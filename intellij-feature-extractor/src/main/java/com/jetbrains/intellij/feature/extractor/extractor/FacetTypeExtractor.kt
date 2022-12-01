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
import org.jetbrains.org.objectweb.asm.tree.MethodInsnNode
import org.jetbrains.org.objectweb.asm.tree.analysis.Value

/*
 * Extracts value returned by FacetType#getStringId from a class extending FacetType.
*/
class FacetTypeExtractor : Extractor {

  private companion object {
    const val FACET_TYPE = "com/intellij/facet/FacetType"
  }

  override fun extract(plugin: IdePlugin, resolver: Resolver): List<ExtensionPointFeatures> {
    return getExtensionPointImplementors(plugin, resolver, ExtensionPoint.FACET_TYPE)
      .mapNotNull { extractFacetTypes(it) }
  }

  private fun extractFacetTypes(classFile: ClassFile): ExtensionPointFeatures? {
    if (classFile.superName != FACET_TYPE) {
      return null
    }

    for (constructorMethod in classFile.methods.filter { it.isConstructor }) {
      val frames = analyzeMethodFrames(constructorMethod) ?: continue

      constructorMethod.instructions.forEachIndexed { index, instruction ->
        if (instruction is MethodInsnNode) {
          if (instruction.name == "<init>" && instruction.owner == FACET_TYPE) {

            val frame = frames[index]

            val value: Value?
            value = when {
              instruction.desc == "(Lcom/intellij/facet/FacetTypeId;Ljava/lang/String;Ljava/lang/String;Lcom/intellij/facet/FacetTypeId;)V" -> frame.getOnStack(2)
              instruction.desc == "(Lcom/intellij/facet/FacetTypeId;Ljava/lang/String;Ljava/lang/String;)V" -> frame.getOnStack(1)
              else -> return@forEachIndexed
            }

            val stringValue = CodeAnalysis().evaluateConstantString(constructorMethod, frames, value)
            if (stringValue != null) {
              return ExtensionPointFeatures(ExtensionPoint.FACET_TYPE, listOf(stringValue))
            }
          }
        }
      }
    }
    return null
  }
}