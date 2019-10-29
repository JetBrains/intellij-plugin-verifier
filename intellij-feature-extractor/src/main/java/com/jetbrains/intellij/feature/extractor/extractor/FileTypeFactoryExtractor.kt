package com.jetbrains.intellij.feature.extractor.extractor

import com.jetbrains.intellij.feature.extractor.ExtensionPoint
import com.jetbrains.intellij.feature.extractor.ExtensionPointFeatures
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.pluginverifier.verifiers.*
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import com.jetbrains.pluginverifier.verifiers.resolution.resolveClassOrNull
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*
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

    private const val FILENAME_MATCHERS = "(Lcom/intellij/openapi/fileTypes/FileType;[Lcom/intellij/openapi/fileTypes/FileNameMatcher;)V"

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
    val arrayProducer = frames[arrayUserInstructionIndex].getOnStack(0) ?: return null
    if (arrayProducer.insns.size != 1) {
      return null
    }

    val anewArrayInstruction = arrayProducer.insns.first()
    if (anewArrayInstruction is TypeInsnNode && anewArrayInstruction.opcode == Opcodes.ANEWARRAY) {
      val newArrayInstructionIndex = methodInstructions.indexOf(anewArrayInstruction)
      if (newArrayInstructionIndex == -1) {
        return null
      }
      return aggregateFileNameMatcherAsArrayElements(
        newArrayInstructionIndex,
        arrayUserInstructionIndex,
        methodInstructions,
        frames,
        method
      )
    }
    return null
  }

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
        if (instructions[pos++].opcode == Opcodes.DUP) {
          val argumentInsn = instructions[pos++]
          if (argumentInsn.opcode == Opcodes.LDC || argumentInsn.opcode == Opcodes.GETSTATIC) {
            val frame = frames[pos]
            val initInvoke = instructions[pos]
            pos++

            if (initInvoke is MethodInsnNode
              && initInvoke.name == "<init>"
              && initInvoke.owner == EXACT_NAME_MATCHER
              && initInvoke.desc == "(Ljava/lang/String;)V") {
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
        if (instructions[pos++].opcode == Opcodes.DUP) {
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
                && initInvoke.desc == "(Ljava/lang/String;Z)V") {

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
        if (instructions[pos++].opcode == Opcodes.DUP) {
          val argumentInsn = instructions[pos++]
          if (argumentInsn.opcode == Opcodes.LDC || argumentInsn.opcode == Opcodes.GETSTATIC) {
            val initInvoke = instructions[pos]
            val frame = frames[pos]
            pos++

            if (initInvoke is MethodInsnNode
              && initInvoke.name == "<init>"
              && initInvoke.owner == EXTENSIONS_MATCHER
              && initInvoke.desc == "(Ljava/lang/String;)V") {

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
      if (instructions[pos++].opcode == Opcodes.DUP) {
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
    if (value !is SourceValue || value.insns == null || value.insns.size != 1) {
      return null
    }
    val first = value.insns.first() as? TypeInsnNode ?: return null
    val className = first.desc.extractClassNameFromDescriptor() ?: return null
    val classFile = resolver.resolveClassOrNull(className) ?: return null

    val method = classFile.methods.find { it.name == "getDefaultExtension" && it.methodParameters.isEmpty() }
      ?: return null
    return CodeAnalysis().evaluateConstantFunctionValue(method)
  }


}