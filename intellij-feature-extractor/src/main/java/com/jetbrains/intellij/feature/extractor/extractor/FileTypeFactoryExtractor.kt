/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.intellij.feature.extractor.extractor

import com.jetbrains.intellij.feature.extractor.ExtensionPoint
import com.jetbrains.intellij.feature.extractor.ExtensionPointFeatures
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.pluginverifier.verifiers.CodeAnalysis
import com.jetbrains.pluginverifier.verifiers.analyzeMethodFrames
import com.jetbrains.pluginverifier.verifiers.extractClassNameFromDescriptor
import com.jetbrains.pluginverifier.verifiers.getOnStack
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import com.jetbrains.pluginverifier.verifiers.resolution.resolveClassOrNull
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.IntInsnNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.TypeInsnNode
import org.objectweb.asm.tree.VarInsnNode
import org.objectweb.asm.tree.analysis.Frame
import org.objectweb.asm.tree.analysis.SourceValue
import org.objectweb.asm.tree.analysis.Value

/**
 * Extracts file extensions passed to consumer of FileTypeFactory.createFileTypes(FileTypeConsumer) from a class extending FileTypeFactory
 */
class FileTypeFactoryExtractor : Extractor {

  companion object {
    private const val FILE_TYPE_FACTORY = "com/intellij/openapi/fileTypes/FileTypeFactory"
    private const val FILE_TYPE_CONSUMER = "com/intellij/openapi/fileTypes/FileTypeConsumer"

    private const val EXPLICIT_EXTENSION = "(Lcom/intellij/openapi/fileTypes/FileType;Ljava/lang/String;)V"
    private const val FILE_TYPE_ONLY = "(Lcom/intellij/openapi/fileTypes/FileType;)V"

    private const val FILENAME_MATCHERS =
      "(Lcom/intellij/openapi/fileTypes/FileType;[Lcom/intellij/openapi/fileTypes/FileNameMatcher;)V"

    private const val EXACT_NAME_MATCHER = "com/intellij/openapi/fileTypes/ExactFileNameMatcher"

    private const val EXTENSIONS_MATCHER = "com/intellij/openapi/fileTypes/ExtensionFileNameMatcher"

    fun parseExtensionsList(extensions: String?): List<String> = splitSemicolonDelimitedList(extensions).map { "*.$it" }

    fun splitSemicolonDelimitedList(semicolonDelimited: String?): List<String> {
      if (semicolonDelimited.isNullOrBlank()) {
        return emptyList()
      }
      return semicolonDelimited.split(';').map(String::trim).filterNot(String::isEmpty)
    }
  }

  override fun extract(plugin: IdePlugin, resolver: Resolver): List<ExtensionPointFeatures> {
    return getExtensionPointImplementors(plugin, resolver, ExtensionPoint.FILE_TYPE_FACTORY)
      .mapNotNull { extractFileTypes(it, resolver) }
  }

  private fun extractFileTypes(classFile: ClassFile, resolver: Resolver): ExtensionPointFeatures? {
    if (classFile.superName != FILE_TYPE_FACTORY) {
      return null
    }
    val method = classFile.methods.find {
      it.name == "createFileTypes" && it.descriptor == "(Lcom/intellij/openapi/fileTypes/FileTypeConsumer;)V" && !it.isAbstract
    } ?: return null

    val frames = analyzeMethodFrames(method) ?: return null

    val result = arrayListOf<String>()
    val instructions = method.instructions
    instructions.forEachIndexed { index, instruction ->
      if (instruction is MethodInsnNode) {

        if (instruction.name == "consume" && instruction.owner == FILE_TYPE_CONSUMER) {

          if (instruction.desc == EXPLICIT_EXTENSION) {
            val frame = frames[index]
            val stringValue = CodeAnalysis().evaluateConstantString(method, frames, frame.getOnStack(0))
            if (stringValue != null) {
              result.addAll(parseExtensionsList(stringValue))
            }
          } else if (instruction.desc == FILE_TYPE_ONLY) {
            val frame = frames[index]
            val fileTypeInstance = frame.getOnStack(0)
            val fromFileType = evaluateExtensionsOfFileType(fileTypeInstance, resolver)
            if (fromFileType != null) {
              result.addAll(parseExtensionsList(fromFileType))
            }
          } else if (instruction.desc == FILENAME_MATCHERS) {
            val extensions = computeExtensionsPassedToFileNameMatcherArray(instructions, index, frames, method)
            if (extensions != null) {
              result.addAll(extensions)
            }
          }
        }
      }
    }
    return ExtensionPointFeatures(ExtensionPoint.FILE_TYPE_FACTORY, result)
  }

  private fun computeExtensionsPassedToFileNameMatcherArray(
    methodInstructions: List<AbstractInsnNode>,
    arrayUserInstructionIndex: Int,
    frames: List<Frame<SourceValue>>,
    method: Method
  ): List<String>? {
    return method.searchMostRecentNewArrayInstructionIndex()?.let { newArrayInstructionIndex ->
      aggregateFileNameMatcherAsArrayElements(
        newArrayInstructionIndex,
        arrayUserInstructionIndex,
        methodInstructions,
        frames,
        method
      )
    }
  }

  private fun Method.searchMostRecentNewArrayInstructionIndex(): Int? = instructions
    .indexOfLast { it is TypeInsnNode && it.opcode == ANEWARRAY }
    .takeIf { it != 1 }

  /**
   * Try to parse the following sequence of instructions:
   * ICONST_k
   * ANEWARRAY 'com/intellij/openapi/fileTypes/FileNameMatcher'
   * <set_element_0>
   * <set_element_1>
   * ...
   * <set_element_(k-1)>
   * INVOKEINTERFACE com/intellij/openapi/fileTypes/FileTypeConsumer.consume (Lcom/intellij/openapi/fileTypes/FileType;[Lcom/intellij/openapi/fileTypes/FileNameMatcher;)V
   *
   * where <set_element_i> consists of the instructions:
   * DUP
   * ICONST_i
   * <block_i>
   * AASTORE
   *
   * where <block_i> represents the element creation. We support only NEW creations (not using the local variables).
   * e.g. block_i may look like this:
   *
   * NEW com/intellij/openapi/fileTypes/ExactFileNameMatcher
   * DUP
   * LDC 'someExtension'
   * INVOKESPECIAL com/intellij/openapi/fileTypes/ExactFileNameMatcher.<init> (Ljava/lang/String;)V
   *
   * in general case <block_i> is a set of instructions which constructs or obtains an instance
   * of the i-th element of the array.
   */
  private fun aggregateFileNameMatcherAsArrayElements(
    newArrayInstructionIndex: Int,
    arrayUserInstructionIndex: Int,
    methodInstructions: List<AbstractInsnNode>,
    frames: List<Frame<SourceValue>>,
    method: Method
  ): List<String> {
    val dummyValue: AbstractInsnNode = object : AbstractInsnNode(-1) {
      override fun getType(): Int = -1

      override fun accept(p0: MethodVisitor?) = Unit

      override fun clone(clonedLabels: MutableMap<LabelNode, LabelNode>?) = this
    }

    //insert dummy instructions to the end to prevent ArrayIndexOutOfBoundsException.
    val instructions = methodInstructions + Array(10) { dummyValue }.toList()

    //skip the ANEWARRAY instruction
    var pos = newArrayInstructionIndex + 1

    /*
    * NEW com/intellij/openapi/fileTypes/ExactFileNameMatcher
    * DUP
    * LDC | GET_STATIC
    * INVOKESPECIAL com/intellij/openapi/fileTypes/ExactFileNameMatcher.<init> (Ljava/lang/String;)V
    */
    fun tryParseExactMatcherConstructorOfOneArgument(): String? {
      val oldPos = pos
      val new = instructions[pos++]
      if (new is TypeInsnNode && new.desc == EXACT_NAME_MATCHER) {
        if (instructions[pos++].opcode == DUP) {
          val argumentInsn = instructions[pos++]
          if (argumentInsn.opcode == Opcodes.LDC || argumentInsn.opcode == Opcodes.GETSTATIC) {
            val frame = frames[pos]
            val initInvoke = instructions[pos]
            pos++

            if (initInvoke is MethodInsnNode
              && initInvoke.name == "<init>"
              && initInvoke.owner == EXACT_NAME_MATCHER
              && initInvoke.desc == "(Ljava/lang/String;)V"
            ) {
              val string = CodeAnalysis().evaluateConstantString(method, frames, frame.getOnStack(0))
              if (string != null) {
                return string
              }
            }
          }
        }
      }
      pos = oldPos
      return null
    }

    /*
    * NEW com/intellij/openapi/fileTypes/ExactFileNameMatcher
    * DUP
    * LDC | GET_STATIC
    * ICONST_z
    * INVOKESPECIAL com/intellij/openapi/fileTypes/ExactFileNameMatcher.<init> (Ljava/lang/String;Z)V
    */
    fun tryParseExactMatcherConstructorOfTwoArguments(): String? {
      val oldPos = pos
      val new = instructions[pos++]
      if (new is TypeInsnNode && new.desc == EXACT_NAME_MATCHER) {
        if (instructions[pos++].opcode == DUP) {
          val argumentInsn = instructions[pos++]
          if (argumentInsn.opcode == Opcodes.LDC || argumentInsn.opcode == Opcodes.GETSTATIC) {
            if (instructions[pos].opcode == Opcodes.ICONST_0 || instructions[pos].opcode == Opcodes.ICONST_1) {
              pos++

              val frame = frames[pos]
              val initInvoke = instructions[pos]
              pos++

              if (initInvoke is MethodInsnNode
                && initInvoke.name == "<init>"
                && initInvoke.owner == EXACT_NAME_MATCHER
                && initInvoke.desc == "(Ljava/lang/String;Z)V"
              ) {

                val string = CodeAnalysis().evaluateConstantString(method, frames, frame.getOnStack(1))
                if (string != null) {
                  return string
                }
              }
            }
          }
        }
      }
      pos = oldPos
      return null
    }


    /*
    * NEW com/intellij/openapi/fileTypes/ExtensionFileNameMatcher
    * DUP
    * LDC | GET_STATIC
    * INVOKESPECIAL com/intellij/openapi/fileTypes/ExtensionFileNameMatcher.<init> (Ljava/lang/String;)V
    */
    fun tryParseExtensionMatcher(): String? {
      val oldPos = pos
      val new = instructions[pos++]
      if (new is TypeInsnNode && new.desc == EXTENSIONS_MATCHER) {
        if (instructions[pos++].opcode == DUP) {
          val argumentInsn = instructions[pos++]
          if (argumentInsn.opcode == Opcodes.LDC || argumentInsn.opcode == Opcodes.GETSTATIC) {
            val initInvoke = instructions[pos]
            val frame = frames[pos]
            pos++

            if (initInvoke is MethodInsnNode
              && initInvoke.name == "<init>"
              && initInvoke.owner == EXTENSIONS_MATCHER
              && initInvoke.desc == "(Ljava/lang/String;)V"
            ) {

              val string = CodeAnalysis().evaluateConstantString(method, frames, frame.getOnStack(0))
              if (string != null) {
                return "*.$string"
              }
            }
          }
        }
      }
      pos = oldPos
      return null
    }

    fun parseBlock(): String? {
      return tryParseExactMatcherConstructorOfOneArgument()
        ?: tryParseExactMatcherConstructorOfTwoArguments()
        ?: tryParseExtensionMatcher()
        ?: return null
    }

    val result = arrayListOf<String>()

    /*
    * DUP
    * ICONST_i
    * <block_i>
    * AASTORE
    */
    fun parseSetElement(i: Int): String? {
      if (instructions[pos++].opcode == DUP) {
        if (i == takeNumberFromIntInstruction(instructions[pos++])) {
          val block = parseBlock() ?: return null
          if (instructions[pos++].opcode == Opcodes.AASTORE) {
            return block
          }
        }
      }
      return null
    }

    var i = 0
    while (pos < arrayUserInstructionIndex) {
      val ithElement = parseSetElement(i++) ?: return result
      result.add(ithElement)
    }
    return result
  }

  private fun takeNumberFromIntInstruction(instruction: AbstractInsnNode): Int? {
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

  /**
   * Extract value returned by com.intellij.openapi.fileTypes.FileType.getDefaultExtension
   */
  private fun evaluateExtensionsOfFileType(value: Value?, resolver: Resolver): String? {
    return value
      .takeIf { it.hasSingleInstruction() }
      ?.let { extractClassName(it as SourceValue) }
      ?.let { resolver.resolveClassOrNull(it) }
      ?.let { it.methods.find { it.name == "getDefaultExtension" && it.methodParameters.isEmpty() } }?.let {
        CodeAnalysis().evaluateConstantFunctionValue(it)
      }
  }

  private fun Value?.hasSingleInstruction(): Boolean = this is SourceValue && insns != null && insns.size == 1

  private fun resolveNew(value: SourceValue): AbstractInsnNode? {
    val instructions = value.insns
    if (instructions.size != 1) {
      return null
    }
    return resolveNew(instructions.first())
  }

  private fun resolveNew(instruction: AbstractInsnNode): AbstractInsnNode? {
    return when {
      instruction.isDup() -> resolveNew(instruction.previous)
      instruction.isNew() -> instruction
      else -> null
    }
  }

  private fun extractClassName(value: SourceValue): String? {
    return extractClassName(value.insns.first())
  }

  private fun extractClassName(instruction: AbstractInsnNode): String? = when {
    instruction.isDup() -> extractClassName(instruction.previous)
    instruction is TypeInsnNode -> instruction.desc.extractClassNameFromDescriptor()
    else -> null
  }

  private fun AbstractInsnNode.isDup() = this is InsnNode && opcode == DUP

  private fun AbstractInsnNode.isNew() = this is TypeInsnNode && opcode == NEW

}

/**
 * Provides a debug string representation of a type instruction.
 *
 * It is used mainly in IDE debugging to better describe ASM nodes.
 */
@Suppress("unused")
fun TypeInsnNode.toDebugString(): String {
  return try {
    when (opcode) {
      NEW -> "NEW $desc"
      ANEWARRAY -> "ANEWARRAY $desc"
      CHECKCAST -> "CHECKLIST $desc"
      INSTANCEOF -> "INSTANCEOF $desc"
      DUP -> "DUP"
      else -> toString()
    }
  } catch (e: Exception) {
    return e.message.toString()
  }
}

/**
 * Provides a debug string representation of a zero-operand instruction.
 *
 * It is used mainly in IDE debugging to better describe ASM nodes.
 */
@Suppress("unused")
fun InsnNode.toDebugString(): String = insnOpCodes[opcode] ?: toString()

fun VarInsnNode.toDebugString(): String = insnOpCodes[opcode] ?: toString()

fun SourceValue.toDebugString(): String {
  return insns.joinToString {
    insnOpCodes[it.opcode] ?: it.toString()
  }
}

private val insnOpCodes = mapOf(
  NOP to "NOP",
  ACONST_NULL to "ACONST_NULL",
  ICONST_M1 to "ICONST_M1",
  ICONST_0 to "ICONST_0",
  ICONST_1 to "ICONST_1",
  ICONST_2 to "ICONST_2",
  ICONST_3 to "ICONST_3",
  ICONST_4 to "ICONST_4",
  ICONST_5 to "ICONST_5",
  LCONST_0 to "LCONST_0",
  LCONST_1 to "LCONST_1",
  FCONST_0 to "FCONST_0",
  FCONST_1 to "FCONST_1",
  FCONST_2 to "FCONST_2",
  DCONST_0 to "DCONST_0",
  DCONST_1 to "DCONST_1",
  ILOAD to "ILOAD",
  LLOAD to "LLOAD",
  FLOAD to "FLOAD",
  DLOAD to "DLOAD",
  ALOAD to "ALOAD",

  IALOAD to "IALOAD",
  LALOAD to "LALOAD",
  FALOAD to "FALOAD",
  DALOAD to "DALOAD",
  AALOAD to "AALOAD",
  BALOAD to "BALOAD",
  CALOAD to "CALOAD",
  SALOAD to "SALOAD",

  ISTORE to "ISTORE",
  LSTORE to "LSTORE",
  FSTORE to "FSTORE",
  DSTORE to "DSTORE",
  ASTORE to "ASTORE",

  IASTORE to "IASTORE",
  LASTORE to "LASTORE",
  FASTORE to "FASTORE",
  DASTORE to "DASTORE",
  AASTORE to "AASTORE",
  BASTORE to "BASTORE",
  CASTORE to "CASTORE",
  SASTORE to "SASTORE",
  POP to "POP",
  POP2 to "POP2",
  DUP to "DUP",
  DUP_X1 to "DUP_X1",
  DUP_X2 to "DUP_X2",
  DUP2 to "DUP2",
  DUP2_X1 to "DUP2_X1",
  DUP2_X2 to "DUP2_X2",
  SWAP to "SWAP",
  IADD to "IADD",
  LADD to "LADD",
  FADD to "FADD",
  DADD to "DADD",
  ISUB to "ISUB",
  LSUB to "LSUB",
  FSUB to "FSUB",
  DSUB to "DSUB",
  IMUL to "IMUL",
  LMUL to "LMUL",
  FMUL to "FMUL",
  DMUL to "DMUL",
  IDIV to "IDIV",
  LDIV to "LDIV",
  FDIV to "FDIV",
  DDIV to "DDIV",
  IREM to "IREM",
  LREM to "LREM",
  FREM to "FREM",
  DREM to "DREM",
  INEG to "INEG",
  LNEG to "LNEG",
  FNEG to "FNEG",
  DNEG to "DNEG",
  ISHL to "ISHL",
  LSHL to "LSHL",
  ISHR to "ISHR",
  LSHR to "LSHR",
  IUSHR to "IUSHR",
  LUSHR to "LUSHR",
  IAND to "IAND",
  LAND to "LAND",
  IOR to "IOR",
  LOR to "LOR",
  IXOR to "IXOR",
  LXOR to "LXOR",
  I2L to "I2L",
  I2F to "I2F",
  I2D to "I2D",
  L2I to "L2I",
  L2F to "L2F",
  L2D to "L2D",
  F2I to "F2I",
  F2L to "F2L",
  F2D to "F2D",
  D2I to "D2I",
  D2L to "D2L",
  D2F to "D2F",
  I2B to "I2B",
  I2C to "I2C",
  I2S to "I2S",
  LCMP to "LCMP",
  FCMPL to "FCMPL",
  FCMPG to "FCMPG",
  DCMPL to "DCMPL",
  DCMPG to "DCMPG",

  RET to "RET",

  IRETURN to "IRETURN",
  LRETURN to "LRETURN",
  FRETURN to "FRETURN",
  DRETURN to "DRETURN",
  ARETURN to "ARETURN",
  RETURN to "RETURN",
  ANEWARRAY to "ANEWARRAY",
  ARRAYLENGTH to "ARRAYLENGTH",
  ATHROW to "ATHROW",
  MONITORENTER to "MONITORENTER",
  MONITOREXIT to "MONITOREXIT"
)