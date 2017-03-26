package com.jetbrains.intellij.feature.extractor.core

import com.intellij.structure.resolvers.Resolver
import org.jetbrains.intellij.plugins.internal.asm.Opcodes
import org.jetbrains.intellij.plugins.internal.asm.Type
import org.jetbrains.intellij.plugins.internal.asm.tree.ClassNode
import org.jetbrains.intellij.plugins.internal.asm.tree.MethodInsnNode
import org.jetbrains.intellij.plugins.internal.asm.tree.analysis.Analyzer
import org.jetbrains.intellij.plugins.internal.asm.tree.analysis.SourceInterpreter

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

    val frames = Analyzer(SourceInterpreter()).analyze(classNode.name, init).toList()
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