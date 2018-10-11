package com.jetbrains.intellij.feature.extractor.core

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import org.objectweb.asm.tree.analysis.Frame
import org.objectweb.asm.tree.analysis.SourceValue
import org.objectweb.asm.tree.analysis.Value

/**
 * Extracts file extensions passed to consumer of FileTypeFactory.createFileTypes(FileTypeConsumer) from a class extending FileTypeFactory
 */
class FileTypeExtractor(resolver: Resolver) : Extractor(resolver) {

  companion object {
    private const val FILE_TYPE_FACTORY = "com/intellij/openapi/fileTypes/FileTypeFactory"
    private const val FILE_TYPE_CONSUMER = "com/intellij/openapi/fileTypes/FileTypeConsumer"

    private const val EXPLICIT_EXTENSION = "(Lcom/intellij/openapi/fileTypes/FileType;Ljava/lang/String;)V"
    private const val FILE_TYPE_ONLY = "(Lcom/intellij/openapi/fileTypes/FileType;)V"

    private const val FILENAME_MATCHERS = "(Lcom/intellij/openapi/fileTypes/FileType;[Lcom/intellij/openapi/fileTypes/FileNameMatcher;)V"

    private const val EXACT_NAME_MATCHER = "com/intellij/openapi/fileTypes/ExactFileNameMatcher"

    private const val EXTENSIONS_MATCHER = "com/intellij/openapi/fileTypes/ExtensionFileNameMatcher"
  }

  override fun extractImpl(classNode: ClassNode): List<String>? {
    if (classNode.superName != FILE_TYPE_FACTORY) {
      return null
    }
    val method = classNode.findMethod { it.name == "createFileTypes" && it.desc == "(Lcom/intellij/openapi/fileTypes/FileTypeConsumer;)V" && !it.isAbstract() }
        ?: return null
    val frames = AnalysisUtil.analyzeMethodFrames(classNode, method)

    val result = arrayListOf<String>()
    extractedAll = true
    var foundAnyConsumeInvocation = false

    val instructions = method.instructionsAsList()
    instructions.forEachIndexed { index, insn ->
      if (insn is MethodInsnNode) {

        if (insn.name == "consume" && insn.owner == FILE_TYPE_CONSUMER) {

          if (insn.desc == EXPLICIT_EXTENSION) {
            foundAnyConsumeInvocation = true
            val frame = frames[index]
            val stringValue = AnalysisUtil.evaluateConstantString(frame.getOnStack(0), resolver, frames, instructions)
            if (stringValue != null) {
              result.addAll(parse(stringValue))
            } else {
              extractedAll = false
            }
          } else if (insn.desc == FILE_TYPE_ONLY) {
            foundAnyConsumeInvocation = true
            val frame = frames[index]
            val fileTypeInstance = frame.getOnStack(0)
            val fromFileType = evaluateExtensionsOfFileType(fileTypeInstance)
            if (fromFileType != null) {
              result.addAll(parse(fromFileType))
            } else {
              extractedAll = false
            }
          } else if (insn.desc == FILENAME_MATCHERS) {
            foundAnyConsumeInvocation = true
            val extensions = computeExtensionsPassedToFileNameMatcherArray(instructions, index, frames)
            if (extensions != null) {
              result.addAll(extensions)
            } else {
              extractedAll = false
            }
          }
        }
      }
    }

    extractedAll = foundAnyConsumeInvocation && extractedAll

    return result
  }

  private fun computeExtensionsPassedToFileNameMatcherArray(
      methodInstructions: List<AbstractInsnNode>,
      arrayUserInstructionIndex: Int,
      frames: List<Frame<SourceValue>>
  ): List<String>? {
    val arrayProducer = frames[arrayUserInstructionIndex].getOnStack(0) ?: return null
    if (arrayProducer !is SourceValue || arrayProducer.insns.size != 1) {
      return null
    }

    val anewArrayInsn = arrayProducer.insns.first()
    if (anewArrayInsn is TypeInsnNode && anewArrayInsn.opcode == Opcodes.ANEWARRAY) {
      val newArrayInsnIndex = methodInstructions.indexOf(anewArrayInsn)
      if (newArrayInsnIndex == -1) {
        return null
      }
      return aggregateFileNameMatcherAsArrayElements(newArrayInsnIndex, arrayUserInstructionIndex, methodInstructions, frames)
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
      newArrayInsnIndex: Int,
      arrayUserInstructionIndex: Int,
      methodInstructions: List<AbstractInsnNode>, frames: List<Frame<SourceValue>>
  ): List<String> {
    val dummyValue: AbstractInsnNode = object : AbstractInsnNode(-1) {
      override fun getType(): Int = -1

      override fun accept(p0: MethodVisitor?) = Unit

      override fun clone(clonedLabels: MutableMap<LabelNode, LabelNode>?) = this
    }

    //insert dummy instructions to the end to prevent ArrayIndexOutOfBoundsException.
    val insns = methodInstructions + dummyValue.replicate(10)

    //skip the ANEWARRAY instruction
    var pos = newArrayInsnIndex + 1

    /*
    * NEW com/intellij/openapi/fileTypes/ExactFileNameMatcher
    * DUP
    * LDC | GET_STATIC
    * INVOKESPECIAL com/intellij/openapi/fileTypes/ExactFileNameMatcher.<init> (Ljava/lang/String;)V
    */
    fun tryParseExactMatcherConstructorOfOneArgument(): String? {
      val oldPos = pos
      val new = insns[pos++]
      if (new is TypeInsnNode && new.desc == EXACT_NAME_MATCHER) {
        if (insns[pos++].opcode == Opcodes.DUP) {
          val argumentInsn = insns[pos++]
          if (argumentInsn.opcode == Opcodes.LDC || argumentInsn.opcode == Opcodes.GETSTATIC) {
            val frame = frames[pos]
            val initInvoke = insns[pos]
            pos++

            if (initInvoke is MethodInsnNode
                && initInvoke.name == "<init>"
                && initInvoke.owner == EXACT_NAME_MATCHER
                && initInvoke.desc == "(Ljava/lang/String;)V") {
              val string = AnalysisUtil.evaluateConstantString(frame.getOnStack(0), resolver, frames, insns)
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
      val new = insns[pos++]
      if (new is TypeInsnNode && new.desc == EXACT_NAME_MATCHER) {
        if (insns[pos++].opcode == Opcodes.DUP) {
          val argumentInsn = insns[pos++]
          if (argumentInsn.opcode == Opcodes.LDC || argumentInsn.opcode == Opcodes.GETSTATIC) {
            if (insns[pos].opcode == Opcodes.ICONST_0 || insns[pos].opcode == Opcodes.ICONST_1) {
              pos++

              val frame = frames[pos]
              val initInvoke = insns[pos]
              pos++

              if (initInvoke is MethodInsnNode
                  && initInvoke.name == "<init>"
                  && initInvoke.owner == EXACT_NAME_MATCHER
                  && initInvoke.desc == "(Ljava/lang/String;Z)V") {

                val string = AnalysisUtil.evaluateConstantString(frame.getOnStack(1), resolver, frames, insns)
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
      val new = insns[pos++]
      if (new is TypeInsnNode && new.desc == EXTENSIONS_MATCHER) {
        if (insns[pos++].opcode == Opcodes.DUP) {
          val argumentInsn = insns[pos++]
          if (argumentInsn.opcode == Opcodes.LDC || argumentInsn.opcode == Opcodes.GETSTATIC) {
            val initInvoke = insns[pos]
            val frame = frames[pos]
            pos++

            if (initInvoke is MethodInsnNode
                && initInvoke.name == "<init>"
                && initInvoke.owner == EXTENSIONS_MATCHER
                && initInvoke.desc == "(Ljava/lang/String;)V") {

              val string = AnalysisUtil.evaluateConstantString(frame.getOnStack(0), resolver, frames, insns)
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
      if (insns[pos++].opcode == Opcodes.DUP) {
        if (i == AnalysisUtil.takeNumberFromIntInstruction(insns[pos++])) {
          val block = parseBlock() ?: return null
          if (insns[pos++].opcode == Opcodes.AASTORE) {
            return block
          }
        }
      }
      return null
    }

    var i = 0
    while (pos < arrayUserInstructionIndex) {
      val ithElement = parseSetElement(i++)
      if (ithElement == null) {
        extractedAll = false
        return result
      }
      result.add(ithElement)
    }
    return result
  }

  private fun parse(semicoloned: String): List<String> = semicoloned.split(';').map(String::trim).filterNot(String::isEmpty).map { "*.$it" }

  /**
   * Extract value returned by com.intellij.openapi.fileTypes.FileType.getDefaultExtension
   */
  private fun evaluateExtensionsOfFileType(value: Value?): String? {
    if (value !is SourceValue || value.insns == null || value.insns.size != 1) {
      return null
    }
    val first = value.insns.first() as? TypeInsnNode ?: return null
    val clazz = resolver.findClass(first.desc) ?: return null
    val method = clazz.findMethod { it.name == "getDefaultExtension" && Type.getArgumentTypes(it.desc).isEmpty() }
        ?: return null
    return AnalysisUtil.extractConstantFunctionValue(clazz, method, resolver)
  }


}