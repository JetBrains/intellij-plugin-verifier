package com.jetbrains.intellij.feature.extractor.extractor

import com.jetbrains.intellij.feature.extractor.ExtensionPoint
import com.jetbrains.intellij.feature.extractor.ExtensionPointFeatures
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.pluginverifier.verifiers.analyzeMethodFrames
import com.jetbrains.pluginverifier.verifiers.evaluateConstantString
import com.jetbrains.pluginverifier.verifiers.getOnStack
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.analysis.Value

/*
 * Extracts value returned by FacetType#getStringId from a class extending FacetType.
*/
class FacetTypeExtractor : Extractor {

  private companion object {
    const val FACET_TYPE = "com/intellij/facet/FacetType"
  }

  override fun extract(plugin: IdePlugin, resolver: Resolver): List<ExtensionPointFeatures> {
    return getExtensionPointImplementors(plugin, resolver, ExtensionPoint.FACET_TYPE)
      .mapNotNull { extractFacetTypes(it, resolver) }
  }

  private fun extractFacetTypes(classFile: ClassFile, resolver: Resolver): ExtensionPointFeatures? {
    if (classFile.superName != FACET_TYPE) {
      return null
    }

    for (constructorMethod in classFile.methods.filter { it.isConstructor }) {
      val frames = analyzeMethodFrames(constructorMethod)

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

            val stringValue = evaluateConstantString(value, resolver, frames, constructorMethod.instructions)
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