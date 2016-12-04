package com.jetbrains.intellij.feature.extractor

import com.intellij.structure.resolvers.Resolver
import com.jetbrains.intellij.feature.extractor.AnalysisUtil.extractConstantFunctionValue
import com.jetbrains.intellij.feature.extractor.AnalysisUtil.extractStringValue
import com.jetbrains.intellij.feature.extractor.AnalysisUtil.findMethod
import org.jetbrains.intellij.plugins.internal.asm.Opcodes
import org.jetbrains.intellij.plugins.internal.asm.Type
import org.jetbrains.intellij.plugins.internal.asm.tree.*
import org.jetbrains.intellij.plugins.internal.asm.tree.analysis.*

private fun MethodNode.isAbstract(): Boolean = this.access and Opcodes.ACC_ABSTRACT != 0

private fun FieldNode.isStatic(): Boolean = this.access and Opcodes.ACC_STATIC != 0

private fun Frame.getOnStack(index: Int): Value? = this.getStack(this.stackSize - 1 - index)

interface Extractor {
  fun extract(classNode: ClassNode): List<String>?
}

class RunConfigurationExtractor(val resolver: Resolver) : Extractor {

  private val CONFIGURATION_BASE = "com/intellij/execution/configurations/ConfigurationTypeBase"

  override fun extract(classNode: ClassNode): List<String>? {
    if (classNode.superName == CONFIGURATION_BASE) {
      val init = findMethod(classNode, { it.name == "<init>" }) ?: return null
      val frames = Analyzer(SourceInterpreter()).analyze(classNode.name, init)
      val superInitIndex = init.instructions.toArray().indexOfLast { it is MethodInsnNode && it.name == "<init>" && it.desc == "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljavax/swing/Icon;)V" }
      if (superInitIndex == -1) {
        return null
      }
      val value = extractStringValue(frames[superInitIndex].getOnStack(3), resolver, frames.toList(), init.instructions.toArray().toList())
      if (value != null) {
        return listOf(value)
      }
      return null
    } else {
      val method = findMethod(classNode, { it.name == "getId" }) ?: return null
      if (method.isAbstract()) {
        return null
      }
      val value = extractConstantFunctionValue(classNode, method, resolver)
      return if (value == null) null else listOf(value)
    }
  }
}

class FacetTypeExtractor(val resolver: Resolver) : Extractor {

  private val FACET_TYPE = "com/intellij/facet/FacetType"

  override fun extract(classNode: ClassNode): List<String>? {
    if (classNode.superName != FACET_TYPE) {
      return null
    }

    @Suppress("UNCHECKED_CAST")
    (classNode.methods as List<MethodNode>).filter { it.name == "<init>" }.forEach { initMethod ->
      val interpreter = SourceInterpreter()
      val frames: List<Frame> = Analyzer(interpreter).analyze(classNode.name, initMethod).toList()

      initMethod.instructions.toArray().forEachIndexed { index, insn ->
        if (insn is MethodInsnNode) {
          if (insn.name == "<init>" && insn.owner == FACET_TYPE) {

            val frame: Frame = frames[index]

            val value: Value?
            if (insn.desc == "(Lcom/intellij/facet/FacetTypeId;Ljava/lang/String;Ljava/lang/String;Lcom/intellij/facet/FacetTypeId;)V") {
              value = frame.getOnStack(2)
            } else if (insn.desc == "(Lcom/intellij/facet/FacetTypeId;Ljava/lang/String;Ljava/lang/String;)V") {
              value = frame.getOnStack(1)
            } else {
              return@forEachIndexed
            }

            val stringValue = extractStringValue(value, resolver, frames, initMethod.instructions.toArray().toList())
            if (stringValue != null) {
              return listOf(stringValue)
            }
          }
        }
      }
    }
    return null
  }
}

class FileTypeExtractor(val resolver: Resolver) : Extractor {

  private val FILE_TYPE_FACTORY = "com/intellij/openapi/fileTypes/FileTypeFactory"

  private val EXPLICIT_EXTENSION = "(Lcom/intellij/openapi/fileTypes/FileType;Ljava/lang/String;)V"
  private val FILE_TYPE_ONLY = "(Lcom/intellij/openapi/fileTypes/FileType;)V"
  private val FILENAME_MATCHERS = "(Lcom/intellij/openapi/fileTypes/FileType;[Lcom/intellij/openapi/fileTypes/FileNameMatcher;)V"

  private val EXACT_NAME_MATCHER = "com/intellij/openapi/fileTypes/ExactFileNameMatcher"

  private val EXTENSIONS_MATCHER = "com/intellij/openapi/fileTypes/ExtensionFileNameMatcher"

  override fun extract(classNode: ClassNode): List<String>? {
    if (classNode.superName != FILE_TYPE_FACTORY) {
      return null
    }
    val method = findMethod(classNode, { it.name == "createFileTypes" && !it.isAbstract() }) ?: return null
    val interpreter = SourceInterpreter()
    val frames = Analyzer(interpreter).analyze(classNode.name, method).toList()

    val result = arrayListOf<String>()

    val instructions = method.instructions.toArray().toList()
    instructions.forEachIndexed { index, insn ->
      if (insn is MethodInsnNode) {

        if (insn.desc == EXPLICIT_EXTENSION) {
          val frame = frames[index]
          val stringValue = extractStringValue(frame.getOnStack(0), resolver, frames, instructions)
          if (stringValue != null) {
            result.addAll(parse(stringValue))
          }
        } else if (insn.desc == FILE_TYPE_ONLY) {
          val frame = frames[index]
          val fileTypeInstance = frame.getOnStack(0)
          val fromFileType = getFromFileClass(fileTypeInstance)
          if (fromFileType != null) {
            result.addAll(parse(fromFileType))
          }
        } else if (insn.desc == FILENAME_MATCHERS) {
          val array = analyzeArray(instructions, index, frames)
          if (array != null) {
            result.addAll(array)
          }
        }
      }
    }

    return result
  }

  private fun analyzeArray(instructions: List<AbstractInsnNode>, consumerIndex: Int, frames: List<Frame>): List<String>? {
    val newArrayInsnIndex = instructions.take(consumerIndex).indexOfLast { it is TypeInsnNode && it.opcode == Opcodes.ANEWARRAY }
    if (newArrayInsnIndex == -1) {
      return null
    }
    val result = arrayListOf<String>()
    for (i in newArrayInsnIndex..consumerIndex) {
      val insn = instructions[i]
      if (insn is MethodInsnNode && insn.name == "<init>") {

        if (insn.owner == EXACT_NAME_MATCHER) {
          val frame = frames[i]
          val value: Value?
          if (insn.desc == "(Ljava/lang/String;)V") {
            value = frame.getOnStack(0)
          } else if (insn.desc == "(Ljava/lang/String;Z)V") {
            value = frame.getOnStack(1)
          } else {
            continue
          }
          val stringValue = extractStringValue(value, resolver, frames, instructions)

          if (stringValue != null) {
            result.add(stringValue)
          }
        } else if (insn.owner == EXTENSIONS_MATCHER) {
          val frame = frames[i]
          if (insn.desc == "(Ljava/lang/String;)V") {
            val value = extractStringValue(frame.getOnStack(0), resolver, frames, instructions)

            if (value != null) {
              if (insn.owner == EXTENSIONS_MATCHER) {
                result.add("*.$value")
              } else {
                result.add(value)
              }
            }
          }
        }
      }
    }
    return result
  }

  private fun parse(semicoloned: String?): List<String> = semicoloned?.split(';')?.map(String::trim)?.map { "*.$it" } ?: emptyList()

  private fun getFromFileClass(value: Value?): String? {
    if (value !is SourceValue || value.insns == null || value.insns.size != 1) {
      return null
    }
    val first = value.insns.first() as? TypeInsnNode ?: return null
    val clazz = resolver.findClass(first.desc) ?: return null
    val method = findMethod(clazz, { it.name == "getDefaultExtension" }) ?: return null
    return extractConstantFunctionValue(clazz, method, resolver)
  }


}

object AnalysisUtil {

  private val STRING_BUILDER = "java/lang/StringBuilder"

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
      return AnalysisUtil.extractStringValue(producer, resolver, frames, methodNode.instructions.toArray().toList())
    }

    return null
  }


  fun extractStringValue(value: Value?, resolver: Resolver, frames: List<Frame>, instructions: List<AbstractInsnNode>): String? {
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
          return analyzeStringBuilder(producer, frames, resolver, instructions)
        } else {
          val classNode = resolver.findClass(producer.owner) ?: return null
          val methodNode = findMethod(classNode, { it.name == producer.name && it.desc == producer.desc }) ?: return null
          return extractConstantFunctionValue(classNode, methodNode, resolver)
        }
      } else if (producer is FieldInsnNode) {
        val classNode = resolver.findClass(producer.owner) ?: return null
        val fieldNode = findField(classNode, { it.name == producer.name && it.desc == producer.desc }) ?: return null
        return extractConstantFieldValue(classNode, fieldNode, resolver)
      }
    }
    return null
  }

  fun extractConstantFieldValue(classNode: ClassNode, fieldNode: FieldNode, resolver: Resolver): String? {
    if (!fieldNode.isStatic()) {
      return null
    }

    if (fieldNode.value is String) {
      return fieldNode.value as String
    }
    val clinit = findMethod(classNode, { it.name == "<clinit>" }) ?: return null
    val frames = Analyzer(SourceInterpreter()).analyze(classNode.name, clinit)
    val instructions = clinit.instructions.toArray().toList()
    val putStaticInstructionIndex = instructions.indexOfLast { it is FieldInsnNode && it.opcode == Opcodes.PUTSTATIC && it.name == fieldNode.name && it.desc == fieldNode.desc }
    return extractStringValue(frames[putStaticInstructionIndex].getOnStack(0), resolver, frames.toList(), instructions)
  }


  fun analyzeStringBuilder(producer: MethodInsnNode,
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
        val value = extractStringValue(appendValue, resolver, frames, instructions) ?: return null
        result.append(value)
      }
    }
    return result.toString()
  }

  @Suppress("UNCHECKED_CAST")
  fun findMethod(classNode: ClassNode, predicate: (MethodNode) -> Boolean): MethodNode? = (classNode.methods as List<MethodNode>).find(predicate)

  @Suppress("UNCHECKED_CAST")
  fun findField(classNode: ClassNode, predicate: (FieldNode) -> Boolean): FieldNode? = (classNode.fields as List<FieldNode>).find(predicate)

}

class ArtifactTypeExtractor(val resolver: Resolver) : Extractor {
  override fun extract(classNode: ClassNode): List<String>? {
    val init = findMethod(classNode, { it.name == "<init>" }) ?: return null
    val instructions = init.instructions.toArray().toList()
    val superInitIndex = instructions.indexOfLast { it is MethodInsnNode && it.opcode == Opcodes.INVOKESPECIAL && it.name == "<init>" && it.owner == classNode.superName }
    if (superInitIndex == -1) {
      return null
    }
    val superInitDesc = (instructions[superInitIndex] as MethodInsnNode).desc
    val argumentsNumber = Type.getArgumentTypes(superInitDesc).size

    val frames = Analyzer(SourceInterpreter()).analyze(classNode.name, init).toList()
    val frame = frames[superInitIndex]
    val value = frame.getOnStack(argumentsNumber - 1)
    val stringValue = extractStringValue(value, resolver, frames, instructions)
    if (stringValue != null) {
      return listOf(stringValue)
    }
    return null
  }

}