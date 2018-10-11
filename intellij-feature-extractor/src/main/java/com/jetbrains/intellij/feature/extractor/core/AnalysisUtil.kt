package com.jetbrains.intellij.feature.extractor.core

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import org.objectweb.asm.tree.analysis.*

object AnalysisUtil {

  private const val STRING_BUILDER = "java/lang/StringBuilder"

  fun analyzeMethodFrames(classNode: ClassNode, methodNode: MethodNode): List<Frame<SourceValue>> =
      Analyzer(SourceInterpreter()).analyze(classNode.name, methodNode).toList()

  fun getMethodParametersNumber(methodNode: MethodNode): Int = Type.getMethodType(methodNode.desc).argumentTypes.size

  fun takeNumberFromIntInstruction(instruction: AbstractInsnNode): Int? {
    if (instruction is InsnNode) {
      return when (instruction.opcode) {
        Opcodes.ICONST_M1 -> -1
        Opcodes.ICONST_0 -> 0
        Opcodes.ICONST_1 -> 1
        Opcodes.ICONST_2 -> 2
        Opcodes.ICONST_3 -> 3
        Opcodes.ICONST_4 -> 4
        Opcodes.ICONST_5 -> 5
        else -> null
      }
    }
    if (instruction is IntInsnNode) {
      return when (instruction.opcode) {
        Opcodes.BIPUSH -> instruction.operand
        else -> null
      }
    }
    return null
  }

  fun extractConstantFunctionValue(classNode: ClassNode, methodNode: MethodNode, resolver: Resolver): String? {
    if (methodNode.isAbstract()) {
      return null
    }

    var producer: Value? = null

    val interpreter = object : SourceInterpreter(Opcodes.ASM7) {
      override fun returnOperation(insn: AbstractInsnNode?, value: SourceValue?, expected: SourceValue?) {
        producer = value
        super.returnOperation(insn, value, expected)
      }
    }

    val frames = Analyzer(interpreter).analyze(classNode.name, methodNode).toList()

    if (producer != null) {
      return evaluateConstantString(producer, resolver, frames, methodNode.instructionsAsList())
    }

    return null
  }


  fun evaluateConstantString(value: Value?, resolver: Resolver, frames: List<Frame<SourceValue>>, instructions: List<AbstractInsnNode>): String? {
    if (value !is SourceValue) {
      return null
    }

    val insns = value.insns ?: return null

    if (insns.size == 1) {
      val producer = insns.first()
      if (producer is LdcInsnNode) {
        if (producer.cst is String) {
          return producer.cst as String
        }
      } else if (producer is MethodInsnNode) {
        if (producer.owner == STRING_BUILDER && producer.name == "toString") {
          return evaluateConcatenatedStringValue(producer, frames, resolver, instructions)
        } else {
          val classNode = resolver.findClass(producer.owner) ?: return null
          val methodNode = classNode.findMethod { it.name == producer.name && it.desc == producer.desc } ?: return null
          return extractConstantFunctionValue(classNode, methodNode, resolver)
        }
      } else if (producer is FieldInsnNode) {
        val classNode = resolver.findClass(producer.owner) ?: return null
        val fieldNode = classNode.findField { it.name == producer.name && it.desc == producer.desc } ?: return null
        return evaluateConstantFieldValue(classNode, fieldNode, resolver)
      }
    }
    return null
  }

  private fun evaluateConstantFieldValue(classNode: ClassNode, fieldNode: FieldNode, resolver: Resolver): String? {
    if (!fieldNode.isStatic()) {
      return null
    }

    if (fieldNode.value is String) {
      return fieldNode.value as String
    }
    val clinit = classNode.findMethod { it.name == "<clinit>" } ?: return null
    val frames = AnalysisUtil.analyzeMethodFrames(classNode, clinit)
    val instructions = clinit.instructionsAsList()
    val putStaticInstructionIndex = instructions.indexOfLast {
      it is FieldInsnNode
          && it.opcode == Opcodes.PUTSTATIC
          && it.owner == classNode.name
          && it.name == fieldNode.name
          && it.desc == fieldNode.desc
    }
    return evaluateConstantString(frames[putStaticInstructionIndex].getOnStack(0), resolver, frames.toList(), instructions)
  }


  private fun evaluateConcatenatedStringValue(
      producer: MethodInsnNode,
      frames: List<Frame<SourceValue>>,
      resolver: Resolver,
      instructions: List<AbstractInsnNode>
  ): String? {
    val producerIndex = instructions.indexOf(producer)
    if (producerIndex == -1) {
      return null
    }
    val initIndex = instructions.take(producerIndex).indexOfLast {
      it is MethodInsnNode && it.name == "<init>" && it.owner == STRING_BUILDER
    }
    val result = StringBuilder()
    for (i in initIndex..producerIndex) {
      val insnNode = instructions[i]
      if (insnNode is MethodInsnNode && insnNode.name == "append" && insnNode.owner == STRING_BUILDER) {
        val frame = frames[i]
        val appendValue = frame.getOnStack(0)
        val value = evaluateConstantString(appendValue, resolver, frames, instructions) ?: return null
        result.append(value)
      }
    }
    return result.toString()
  }

}