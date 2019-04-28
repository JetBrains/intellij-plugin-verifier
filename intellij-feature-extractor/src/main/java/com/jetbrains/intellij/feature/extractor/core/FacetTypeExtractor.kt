package com.jetbrains.intellij.feature.extractor.core

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.analysis.Value

/*
 * Extracts value returned by FacetType#getStringId from a class extending FacetType.
*/
class FacetTypeExtractor(resolver: Resolver) : Extractor(resolver) {

  private companion object {
    const val FACET_TYPE = "com/intellij/facet/FacetType"
  }

  override fun extractImpl(classNode: ClassNode): List<String>? {
    if (classNode.superName != FACET_TYPE) {
      return null
    }

    classNode.methods.filter { it.name == "<init>" }.forEach { initMethod ->
      val frames = AnalysisUtil.analyzeMethodFrames(classNode, initMethod)

      initMethod.instructions.toArray().forEachIndexed { index, insn ->
        if (insn is MethodInsnNode) {
          if (insn.name == "<init>" && insn.owner == FACET_TYPE) {

            val frame = frames[index]

            val value: Value?
            value = when {
              insn.desc == "(Lcom/intellij/facet/FacetTypeId;Ljava/lang/String;Ljava/lang/String;Lcom/intellij/facet/FacetTypeId;)V" -> frame.getOnStack(2)
              insn.desc == "(Lcom/intellij/facet/FacetTypeId;Ljava/lang/String;Ljava/lang/String;)V" -> frame.getOnStack(1)
              else -> return@forEachIndexed
            }

            val stringValue = AnalysisUtil.evaluateConstantString(value, resolver, frames, initMethod.instructionsAsList())
            if (stringValue != null) {
              extractedAll = true
              return listOf(stringValue)
            }
          }
        }
      }
    }
    return null
  }
}