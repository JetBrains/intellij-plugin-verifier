package com.jetbrains.intellij.feature.extractor.extractor

import com.jetbrains.intellij.feature.extractor.ExtensionPoint
import com.jetbrains.intellij.feature.extractor.ExtensionPointFeatures
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.pluginverifier.verifiers.analyzeMethodFrames
import com.jetbrains.pluginverifier.verifiers.evaluateConstantString
import com.jetbrains.pluginverifier.verifiers.getOnStack
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
      .flatMap { extractArtifactTypes(it, resolver) }

  private fun extractArtifactTypes(classFile: ClassFile, resolver: Resolver): List<ExtensionPointFeatures> {
    return classFile.methods.filter { it.isConstructor }
      .mapNotNull { extractArtifactTypesFromConstructor(it, classFile, resolver) }
      .toList()
  }

  private fun extractArtifactTypesFromConstructor(
    classConstructor: Method,
    classFile: ClassFile,
    resolver: Resolver
  ): ExtensionPointFeatures? {
    val instructions = classConstructor.instructions
    val superInitIndex = instructions.indexOfLast {
      it is MethodInsnNode && it.opcode == Opcodes.INVOKESPECIAL && it.name == "<init>" && it.owner == classFile.superName
    }
    if (superInitIndex == -1) {
      return null
    }
    val superInitDesc = (instructions[superInitIndex] as? MethodInsnNode)?.desc ?: return null
    val argumentsNumber = Type.getArgumentTypes(superInitDesc).size

    val frames = analyzeMethodFrames(classConstructor)
    val frame = frames[superInitIndex]
    val value = frame.getOnStack(argumentsNumber - 1)
    val stringValue = evaluateConstantString(value, resolver, frames, instructions)
    if (stringValue != null) {
      return ExtensionPointFeatures(ExtensionPoint.ARTIFACT_TYPE, listOf(stringValue))
    }
    return null
  }

}