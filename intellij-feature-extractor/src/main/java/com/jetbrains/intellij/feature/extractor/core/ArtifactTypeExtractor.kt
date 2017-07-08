package com.jetbrains.intellij.feature.extractor.core

import com.intellij.structure.resolvers.Resolver
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode

/**
 * Extracts value returned by ArtifactType#getId() from a class extending ArtifactType.
 */
class ArtifactTypeExtractor(resolver: Resolver) : Extractor(resolver) {
  override fun extractImpl(classNode: ClassNode): List<String>? {
    val init = classNode.findMethod({ it.name == "<init>" }) ?: return null
    val instructions = init.instructionsAsList()
    val superInitIndex = instructions.indexOfLast { it is MethodInsnNode && it.opcode == Opcodes.INVOKESPECIAL && it.name == "<init>" && it.owner == classNode.superName }
    if (superInitIndex == -1) {
      return null
    }
    val superInitDesc = (instructions[superInitIndex] as MethodInsnNode).desc
    val argumentsNumber = Type.getArgumentTypes(superInitDesc).size

    val frames = AnalysisUtil.analyzeMethodFrames(classNode, init)
    val frame = frames[superInitIndex]
    val value = frame.getOnStack(argumentsNumber - 1)
    val stringValue = AnalysisUtil.evaluateConstantString(value, resolver, frames, instructions)
    if (stringValue != null) {
      extractedAll = true
      return listOf(stringValue)
    }
    return null
  }

}