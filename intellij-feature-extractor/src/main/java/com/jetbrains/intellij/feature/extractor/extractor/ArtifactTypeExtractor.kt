package com.jetbrains.intellij.feature.extractor.extractor

import com.jetbrains.intellij.feature.extractor.*
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode

/**
 * Extracts value returned by ArtifactType#getId() from a class extending ArtifactType.
 */
class ArtifactTypeExtractor : Extractor {

  override fun extract(plugin: IdePlugin, resolver: Resolver) =
      getExtensionPointImplementors(plugin, resolver, ExtensionPoint.ARTIFACT_TYPE)
          .flatMap { extractArtifactTypes(it, resolver) }

  private fun extractArtifactTypes(classNode: ClassNode, resolver: Resolver): List<ExtensionPointFeatures> {
    return classNode.findMethods { it.isConstructor }
        .mapNotNull { extractArtifactTypesFromConstructor(it, classNode, resolver) }
  }

  private fun extractArtifactTypesFromConstructor(
      classConstructor: MethodNode,
      classNode: ClassNode,
      resolver: Resolver
  ): ExtensionPointFeatures? {
    val instructions = classConstructor.instructionsAsList()
    val superInitIndex = instructions.indexOfLast {
      it is MethodInsnNode && it.opcode == Opcodes.INVOKESPECIAL && it.name == "<init>" && it.owner == classNode.superName
    }
    if (superInitIndex == -1) {
      return null
    }
    val superInitDesc = (instructions[superInitIndex] as MethodInsnNode).desc
    val argumentsNumber = Type.getArgumentTypes(superInitDesc).size

    val frames = AnalysisUtil.analyzeMethodFrames(classNode, classConstructor)
    val frame = frames[superInitIndex]
    val value = frame.getOnStack(argumentsNumber - 1)
    val stringValue = AnalysisUtil.evaluateConstantString(value, resolver, frames, instructions)
    if (stringValue != null) {
      return ExtensionPointFeatures(ExtensionPoint.ARTIFACT_TYPE, listOf(stringValue))
    }
    return null
  }

}