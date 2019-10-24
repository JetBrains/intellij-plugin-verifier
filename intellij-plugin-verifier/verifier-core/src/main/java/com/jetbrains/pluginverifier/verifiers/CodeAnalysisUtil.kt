package com.jetbrains.pluginverifier.verifiers

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.classes.utils.AsmUtil
import com.jetbrains.pluginverifier.verifiers.resolution.*
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*
import org.objectweb.asm.tree.analysis.*

fun analyzeMethodFrames(method: Method, interpreter: Interpreter<SourceValue> = SourceInterpreter()): List<Frame<SourceValue>> =
  if (method is MethodAsm) {
    Analyzer(interpreter).analyze(method.containingClassFile.name, method.asmNode).toList()
  } else {
    emptyList()
  }

fun Frame<SourceValue>.getOnStack(index: Int): Value? =
  getStack(stackSize - 1 - index)

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

fun evaluateConstantString(
  value: Value?,
  resolver: Resolver,
  frames: List<Frame<SourceValue>>,
  instructions: List<AbstractInsnNode>
): String? {
  if (value !is SourceValue) {
    return null
  }

  val sourceInstructions = value.insns ?: return null

  if (sourceInstructions.size == 1) {
    val producer = sourceInstructions.first()
    if (producer is LdcInsnNode) {
      if (producer.cst is String) {
        return producer.cst as String
      }
    } else if (producer is MethodInsnNode) {
      if (producer.owner == "java/lang/StringBuilder" && producer.name == "toString") {
        return evaluateConcatenatedStringValue(producer, frames, resolver, instructions)
      } else {
        val classNode = resolver.resolveClassOrNull(producer.owner) ?: return null
        val methodAsm = classNode.methods.find { it.name == producer.name && it.descriptor == producer.desc }
          ?: return null
        return extractConstantFunctionValue(methodAsm, resolver)
      }
    } else if (producer is FieldInsnNode) {
      val classFile = resolver.resolveClassOrNull(producer.owner) ?: return null
      val fieldNode = classFile.fields.find { it.name == producer.name && it.descriptor == producer.desc }
        ?: return null
      return evaluateConstantFieldValue(classFile, fieldNode, resolver)
    }
  }
  return null
}


fun extractConstantFunctionValue(method: Method, resolver: Resolver): String? {
  if (method.isAbstract) {
    return null
  }

  var producer: Value? = null

  val interpreter = object : SourceInterpreter(AsmUtil.ASM_API_LEVEL) {
    override fun returnOperation(insn: AbstractInsnNode?, value: SourceValue?, expected: SourceValue?) {
      producer = value
      super.returnOperation(insn, value, expected)
    }
  }

  val frames = analyzeMethodFrames(method, interpreter = interpreter)

  if (producer != null) {
    return evaluateConstantString(producer, resolver, frames, method.instructions)
  }

  return null
}

private fun evaluateConstantFieldValue(
  classFile: ClassFile,
  field: Field,
  resolver: Resolver
): String? {
  if (!field.isStatic) {
    return null
  }

  if (field.initialValue is String) {
    return field.initialValue as String
  }

  val classInitializer = classFile.methods.find { it.name == "<clinit>" } ?: return null
  val frames = analyzeMethodFrames(classInitializer)

  val instructions = classInitializer.instructions
  val putStaticInstructionIndex = instructions.indexOfLast {
    it is FieldInsnNode
      && it.opcode == Opcodes.PUTSTATIC
      && it.owner == classFile.name
      && it.name == field.name
      && it.desc == field.descriptor
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
    it is MethodInsnNode && it.name == "<init>" && it.owner == "java/lang/StringBuilder"
  }
  val result = StringBuilder()
  for (i in initIndex..producerIndex) {
    val instructionNode = instructions[i]
    if (instructionNode is MethodInsnNode && instructionNode.name == "append" && instructionNode.owner == "java/lang/StringBuilder") {
      val frame = frames[i]
      val appendValue = frame.getOnStack(0)
      val value = evaluateConstantString(appendValue, resolver, frames, instructions)
        ?: return null
      result.append(value)
    }
  }
  return result.toString()
}