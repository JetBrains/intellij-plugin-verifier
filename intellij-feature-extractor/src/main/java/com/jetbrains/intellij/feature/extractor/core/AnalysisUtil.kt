package com.jetbrains.intellij.feature.extractor.core

import com.intellij.structure.resolvers.Resolver
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*
import org.objectweb.asm.tree.analysis.*

object AnalysisUtil {

  private val STRING_BUILDER = "java/lang/StringBuilder"

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

    val interpreter = object : SourceInterpreter() {
      override fun ternaryOperation(p0: AbstractInsnNode?, p1: Value?, p2: Value?, p3: Value?): Value {
        return super.ternaryOperation(p0, p1 as SourceValue, p2 as SourceValue, p3 as SourceValue)
      }

      override fun merge(p0: Value?, p1: Value?): Value {
        return super.merge(p0 as SourceValue, p1 as SourceValue)
      }

      override fun returnOperation(p0: AbstractInsnNode?, p1: Value?, p2: Value?) {
        producer = p1
        return super.returnOperation(p0, p1 as SourceValue, p2 as SourceValue)
      }

      override fun unaryOperation(p0: AbstractInsnNode?, p1: Value?): Value {
        return super.unaryOperation(p0, p1 as SourceValue)
      }

      override fun binaryOperation(p0: AbstractInsnNode?, p1: Value?, p2: Value?): Value {
        return super.binaryOperation(p0, p1 as SourceValue, p2 as SourceValue)
      }

      override fun copyOperation(p0: AbstractInsnNode?, p1: Value?): Value {
        return super.copyOperation(p0, p1 as SourceValue)
      }

    }

    val analyzer = Analyzer(interpreter)
    val frames = analyzer.analyze(classNode.name, methodNode).toList()

    if (producer != null) {
      return evaluateConstantString(producer, resolver, frames, methodNode.instructionsAsList())
    }

    return null
  }


  fun evaluateConstantString(value: Value?, resolver: Resolver, frames: List<Frame>, instructions: List<AbstractInsnNode>): String? {
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
          val methodNode = classNode.findMethod({ it.name == producer.name && it.desc == producer.desc }) ?: return null
          return extractConstantFunctionValue(classNode, methodNode, resolver)
        }
      } else if (producer is FieldInsnNode) {
        val classNode = resolver.findClass(producer.owner) ?: return null
        val fieldNode = classNode.findField({ it.name == producer.name && it.desc == producer.desc }) ?: return null
        return evaluateConstantFieldValue(classNode, fieldNode, resolver)
      }
    }
    return null
  }

  fun evaluateConstantFieldValue(classNode: ClassNode, fieldNode: FieldNode, resolver: Resolver): String? {
    if (!fieldNode.isStatic()) {
      return null
    }

    if (fieldNode.value is String) {
      return fieldNode.value as String
    }
    val clinit = classNode.findMethod({ it.name == "<clinit>" }) ?: return null
    val frames = Analyzer(SourceInterpreter()).analyze(classNode.name, clinit)
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


  fun evaluateConcatenatedStringValue(producer: MethodInsnNode,
                                      frames: List<Frame>,
                                      resolver: Resolver,
                                      instructions: List<AbstractInsnNode>): String? {
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