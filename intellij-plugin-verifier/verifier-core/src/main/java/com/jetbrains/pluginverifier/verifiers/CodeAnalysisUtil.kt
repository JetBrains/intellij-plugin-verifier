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
      val producer = sourceInstructions.first()
      return evaluateInstructionConstantString(producer, analyzedMethod, frames)
    }
    return null
  }


  private fun evaluateInstructionConstantString(
    instruction: AbstractInsnNode,
    analyzedMethod: Method,
    frames: List<Frame<SourceValue>>
  ): String? {
    when (instruction) {
      is LdcInsnNode -> {
        if (instruction.cst is String) {
          return instruction.cst as String
        }
      }
      is MethodInsnNode -> {
        if (instruction.owner == "java/lang/StringBuilder" && instruction.name == "toString") {
          val instructions = analyzedMethod.instructions
          val toStringIndex = instructions.indexOf(instruction)
          if (toStringIndex == -1) {
            return null
          }
          val initIndex = instructions.take(toStringIndex).indexOfLast {
            it is MethodInsnNode && it.name == "<init>" && it.owner == "java/lang/StringBuilder"
          }
          if (initIndex == -1) {
            return null
          }
          val stringBuildingInstructions = instructions.subList(initIndex + 1, toStringIndex)
          return evaluateConcatenatedStringValue(frames, analyzedMethod, stringBuildingInstructions)
        } else if (instruction.owner == analyzedMethod.containingClassFile.name) {
          val selfMethod = analyzedMethod.containingClassFile.methods.find {
            it.name == instruction.name && it.descriptor == instruction.desc
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
        if (instruction.owner == analyzedMethod.containingClassFile.name) {
          val fieldNode = analyzedMethod.containingClassFile.fields.find {
            it.name == instruction.name && it.descriptor == instruction.desc
          } ?: return null
          return evaluateConstantFieldValue(fieldNode)
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
      val predicate: (AbstractInsnNode) -> Boolean = {
        it is FieldInsnNode
          && it.opcode == Opcodes.PUTSTATIC
          && it.owner == classFile.name
          && it.name == field.name
          && it.desc == field.descriptor
      }
      if (instructions.count(predicate) != 1) {
        return null
      }
      val putStaticInstructionIndex = instructions.indexOfLast(predicate)
      val frame = frames.getOrNull(putStaticInstructionIndex) ?: return null
      return evaluateConstantString(classInitializer, frames, frame.getOnStack(0))
    } finally {
      inVisitFields.removeLast()
    }
  }


  /**
   * Analyzes bytecode corresponding to String built with StringBuilder:
   * ```
   * LDC "One"
   * INVOKEVIRTUAL java/lang/StringBuilder.append (Ljava/lang/String;)Ljava/lang/StringBuilder;
   * ALOAD 0
   * INVOKEVIRTUAL some/SomeClass.someConstantFunction ()Ljava/lang/String;
   * INVOKEVIRTUAL java/lang/StringBuilder.append (Ljava/lang/String;)Ljava/lang/StringBuilder;
   * GETSTATIC     some/SomeClass.SOME_STATIC_FINAL_CONSTANT : Ljava/lang/String;
   * INVOKEVIRTUAL java/lang/StringBuilder.append (Ljava/lang/String;)Ljava/lang/StringBuilder;
   * ```
   *
   * It firstly splits the instructions by `StringBuilder.append()`:
   * ```
   * LDC         "One"
   * INVOKE      someConstantFunction()
   * GETSTATIC   SOME_STATIC_FINAL_CONSTANT
   * ```
   *
   * Then constructs the result: "One" + someConstantFunction() + SOME_STATIC_FINAL_CONSTANT
   */
  private fun evaluateConcatenatedStringValue(
    frames: List<Frame<SourceValue>>,
    analyzedMethod: Method,
    stringBuildingInstructions: List<AbstractInsnNode>
  ): String? {

    val isAppendInstruction = { ins: AbstractInsnNode ->
      ins is MethodInsnNode && ins.name == "append" && ins.owner == "java/lang/StringBuilder"
    }

    fun evaluateAppendInstructions(instructions: List<AbstractInsnNode>): String? {
      if (instructions.isEmpty()) {
        return null
      }
      if (instructions.size == 1) {
        val single = instructions.single()
        return evaluateInstructionConstantString(single, analyzedMethod, frames)
      }
      if (instructions.size == 2) {
        val first = instructions.first()
        val second = instructions.last()
        if (first is VarInsnNode && first.`var` == 0 && !analyzedMethod.isStatic) {
          return evaluateInstructionConstantString(second, analyzedMethod, frames)
        }
      }
      return null
    }

    val stringResult = StringBuilder()
    val nonAppendInstructionParts = stringBuildingInstructions.splitByPredicate(isAppendInstruction)
    for (appendInstructionsPart in nonAppendInstructionParts) {
      val string = evaluateAppendInstructions(appendInstructionsPart)
        ?: return null
      stringResult.append(string)
    }

    return stringResult.toString()
  }

  private fun <T> List<T>.splitByPredicate(predicate: (T) -> Boolean): List<List<T>> {
    val result = arrayListOf<List<T>>()
    val current = arrayListOf<T>()
    for (t in this) {
      if (predicate(t)) {
        result += current.toList()
        current.clear()
      } else {
        current += t
      }
    }
    return result
  }
}