package com.jetbrains.pluginverifier.verifiers

import com.jetbrains.pluginverifier.verifiers.resolution.Field
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import com.jetbrains.pluginverifier.verifiers.resolution.MethodAsm
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*
import org.objectweb.asm.tree.analysis.Analyzer
import org.objectweb.asm.tree.analysis.Frame
import org.objectweb.asm.tree.analysis.SourceInterpreter
import org.objectweb.asm.tree.analysis.SourceValue
import java.util.*

fun analyzeMethodFrames(method: Method): List<Frame<SourceValue>>? =
  if (method is MethodAsm) {
    Analyzer(SourceInterpreter()).analyze(method.containingClassFile.name, method.asmNode).toList()
  } else {
    null
  }

fun Frame<SourceValue>.getOnStack(index: Int): SourceValue? =
  getStack(stackSize - 1 - index)

class CodeAnalysis {

  private val inVisitMethods: Deque<Method> = LinkedList()

  private val inVisitFields: Deque<Field> = LinkedList()

  fun evaluateConstantString(
    analyzedMethod: Method,
    instructionIndex: Int,
    onStackIndex: Int
  ): String? {
    val frames = analyzeMethodFrames(analyzedMethod) ?: return null
    val frame = frames.getOrNull(instructionIndex) ?: return null
    val sourceValue = frame.getOnStack(onStackIndex) ?: return null
    return evaluateConstantString(analyzedMethod, frames, sourceValue)
  }

  fun evaluateConstantString(
    analyzedMethod: Method,
    frames: List<Frame<SourceValue>>,
    sourceValue: SourceValue?
  ): String? {
    val sourceInstructions = sourceValue?.insns ?: return null
    if (sourceInstructions.size == 1) {
      when (val producer = sourceInstructions.first()) {
        is LdcInsnNode -> {
          if (producer.cst is String) {
            return producer.cst as String
          }
        }
        is MethodInsnNode -> {
          if (producer.owner == "java/lang/StringBuilder" && producer.name == "toString") {
            val instructions = analyzedMethod.instructions
            val producerIndex = instructions.indexOf(producer)
            if (producerIndex == -1) {
              return null
            }
            val initIndex = instructions.take(producerIndex).indexOfLast {
              it is MethodInsnNode && it.name == "<init>" && it.owner == "java/lang/StringBuilder"
            }
            if (initIndex == -1) {
              return null
            }
            return evaluateConcatenatedStringValue(initIndex, producerIndex, frames, analyzedMethod)
          } else if (producer.owner == analyzedMethod.containingClassFile.name) {
            val selfMethod = analyzedMethod.containingClassFile.methods.find {
              it.name == producer.name && it.descriptor == producer.desc
            }
            if (selfMethod != null) {
              val cantBeOverridden = selfMethod.isStatic || selfMethod.isPrivate || selfMethod.isFinal
              if (cantBeOverridden) {
                return evaluateConstantFunctionValue(selfMethod)
              }
            }
          }
        }
        is FieldInsnNode -> {
          if (producer.owner == analyzedMethod.containingClassFile.name) {
            val fieldNode = analyzedMethod.containingClassFile.fields.find {
              it.name == producer.name && it.descriptor == producer.desc
            } ?: return null
            return evaluateConstantFieldValue(fieldNode)
          }
        }
      }
    }
    return null
  }


  fun evaluateConstantFunctionValue(method: Method): String? {
    if (method.isAbstract || method.descriptor != "()Ljava/lang/String;") {
      return null
    }
    if (inVisitMethods.any { it.name == method.name && it.descriptor == method.descriptor && it.containingClassFile.name == method.containingClassFile.name }) {
      return null
    }
    inVisitMethods.addLast(method)
    try {
      val instructions = method.instructions.dropLastWhile { it is LabelNode || it is LineNumberNode }
      if (instructions.isEmpty()) {
        return null
      }
      val lastInstruction = instructions.last()
      if (lastInstruction !is InsnNode || lastInstruction.opcode != Opcodes.ARETURN) {
        return null
      }
      if (instructions.count { it is InsnNode && Opcodes.IRETURN <= it.opcode && it.opcode <= Opcodes.RETURN } > 1) {
        return null
      }
      return evaluateConstantString(method, instructions.size - 1, 0)
    } finally {
      inVisitMethods.removeLast()
    }
  }

  private fun evaluateConstantFieldValue(field: Field): String? {
    if (!field.isStatic || !field.isFinal || field.descriptor != "Ljava/lang/String;") {
      return null
    }

    if (field.initialValue is String) {
      return field.initialValue as String
    }

    if (inVisitFields.any { it.name == field.name && it.descriptor == field.descriptor && it.containingClassFile.name == field.containingClassFile.name }) {
      return null
    }
    inVisitFields.addLast(field)
    try {
      val classFile = field.containingClassFile
      val classInitializer = classFile.methods.find { it.name == "<clinit>" } ?: return null
      val frames = analyzeMethodFrames(classInitializer) ?: return null

      val instructions = classInitializer.instructions
      val putStaticInstructionIndex = instructions.indexOfLast {
        it is FieldInsnNode
          && it.opcode == Opcodes.PUTSTATIC
          && it.owner == classFile.name
          && it.name == field.name
          && it.desc == field.descriptor
      }
      val frame = frames.getOrNull(putStaticInstructionIndex) ?: return null
      return evaluateConstantString(classInitializer, frames, frame.getOnStack(0))
    } finally {
      inVisitFields.removeLast()
    }
  }


  /**
   * Analyzes bytecode corresponding to:
   * ```
   * sb = new StringBuilder()  //fromIndex
   * sb.append("one")
   * sb.append("two")
   * sb.append("three")
   * sb.toString()             //toIndex
   * ```
   */
  private fun evaluateConcatenatedStringValue(
    fromIndex: Int,
    toIndex: Int,
    frames: List<Frame<SourceValue>>,
    analyzedMethod: Method
  ): String? {
    val instructions = analyzedMethod.instructions
    val result = StringBuilder()
    for (i in fromIndex until toIndex) {
      val instructionNode = instructions.getOrNull(i) ?: return null
      if (instructionNode is MethodInsnNode && instructionNode.name == "append" && instructionNode.owner == "java/lang/StringBuilder") {
        val frame = frames.getOrNull(i) ?: return null
        val appendValue = frame.getOnStack(0) ?: return null
        val value = evaluateConstantString(analyzedMethod, frames, appendValue) ?: return null
        result.append(value)
      }
    }
    return result.toString()
  }
}