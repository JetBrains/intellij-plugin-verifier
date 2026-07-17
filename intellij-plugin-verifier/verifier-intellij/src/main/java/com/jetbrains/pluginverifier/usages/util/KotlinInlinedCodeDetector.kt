/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.usages.util

import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileAsm
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import com.jetbrains.pluginverifier.verifiers.resolution.resolveClassOrNull
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.LineNumberNode
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap

private const val SOURCE_DEBUG_EXTENSION_ANNOTATION = "Lkotlin/jvm/internal/SourceDebugExtension;"

/**
 * Detects bytecode instructions that the Kotlin compiler copied into a plugin's class file
 * by inlining an `inline fun` declared outside the plugin ([MP-7133](https://youtrack.jetbrains.com/issue/MP-7133)).
 * API usages carried by such instructions must not be attributed to the plugin:
 * its author only ever called the public function.
 *
 * Detection relies on the SMAP (JSR-45 source map) that the Kotlin compiler emits for every
 * class containing inlined code, mapping each output line to the class it was inlined from.
 * The SMAP is read from the `SourceDebugExtension` attribute or, if stripped, from the
 * `kotlin.jvm.internal.SourceDebugExtension` annotation where Kotlin 1.8+ duplicates it.
 * Without debug info, inlined instructions are indistinguishable and nothing is detected.
 */
class KotlinInlinedCodeDetector {
  private val smapCache = ConcurrentHashMap<String, Optional<Smap>>()

  /**
   * Returns `true` if [instructionNode] of [callerMethod] was inlined from an `inline fun`
   * declared outside the verified plugin; `false` whenever this cannot be established reliably
   * (no line numbers, no SMAP, unresolvable origin, or the plugin's own inline function).
   */
  fun isInlinedFromOutsidePlugin(
    instructionNode: AbstractInsnNode,
    callerMethod: Method,
    context: VerificationContext
  ): Boolean {
    val callerClass = callerMethod.containingClassFile as? ClassFileAsm ?: return false
    val line = findLineNumber(instructionNode) ?: return false
    val smap = smapCache.computeIfAbsent(callerClass.name) {
      Optional.ofNullable(Smap.parse(callerClass.sourceDebugInfo()))
    }.orElse(null) ?: return false
    val originClassName = smap.sourceClassOf(line) ?: return false
    if (originClassName == callerClass.name) return false
    val originClass = context.classResolver.resolveClassOrNull(originClassName) ?: return false
    return !context.isFromVerifiedPlugin(originClass)
  }

  private fun findLineNumber(instructionNode: AbstractInsnNode): Int? {
    var node: AbstractInsnNode? = instructionNode
    while (node != null) {
      if (node is LineNumberNode) return node.line
      node = node.previous
    }
    return null
  }

  private fun ClassFileAsm.sourceDebugInfo(): String? =
    asmNode.sourceDebug ?: sourceDebugExtensionAnnotationValue()

  private fun ClassFileAsm.sourceDebugExtensionAnnotationValue(): String? {
    val annotation = asmNode.invisibleAnnotations.orEmpty()
      .find { it.desc == SOURCE_DEBUG_EXTENSION_ANNOTATION } ?: return null
    val values = annotation.values ?: return null
    val valueIndex = (values.indices step 2).find { values[it] == "value" } ?: return null
    val parts = (values.getOrNull(valueIndex + 1) as? List<*>)?.filterIsInstance<String>() ?: return null
    return parts.joinToString("").takeIf { it.isNotEmpty() }
  }
}

/**
 * The Kotlin section of a JSR-45 source map (SMAP): which source class each output line comes from.
 */
internal class Smap private constructor(
  private val sourceClassesByFileId: Map<Int, String>,
  private val lineRanges: List<LineRange>
) {

  /** The JVM internal name of the class whose source produced the given output line, if known. */
  fun sourceClassOf(outputLine: Int): String? =
    lineRanges.lastOrNull { outputLine in it.outputStart..it.outputEnd }
      ?.let { sourceClassesByFileId[it.fileId] }

  private data class LineRange(val outputStart: Int, val outputEnd: Int, val fileId: Int)

  companion object {
    // InputStartLine [ '#' LineFileID ] [ ',' RepeatCount ] ':' OutputStartLine [ ',' OutputLineIncrement ]
    private val LINE_INFO = Regex("""(\d+)(?:#(\d+))?(?:,(\d+))?:(\d+)(?:,(\d+))?""")

    fun parse(sourceDebug: String?): Smap? {
      val lines = sourceDebug?.lines() ?: return null
      if (lines.firstOrNull()?.trim() != "SMAP") return null
      var i = lines.indexOfFirst { it.trim() == "*S Kotlin" }
      if (i < 0) return null
      i++

      val sourceClasses = mutableMapOf<Int, String>()
      val ranges = mutableListOf<LineRange>()
      var section = ""
      var previousFileId = 1
      while (i < lines.size) {
        val line = lines[i].trim()
        when {
          line.startsWith("*S ") || line == "*E" -> break
          line == "*F" || line == "*L" -> section = line
          section == "*F" && line.startsWith("+ ") -> {
            // "+ <id> <file name>" followed by a line with the source class internal name
            val id = line.removePrefix("+ ").substringBefore(' ').toIntOrNull()
            val sourceClass = lines.getOrNull(i + 1)?.trim()
            if (id != null && !sourceClass.isNullOrEmpty()) {
              sourceClasses[id] = sourceClass
            }
            i++
          }
          section == "*L" -> {
            val match = LINE_INFO.matchEntire(line)
            if (match != null) {
              val (_, fileIdText, repeatText, outputStartText, incrementText) = match.destructured
              val fileId = fileIdText.toIntOrNull() ?: previousFileId
              previousFileId = fileId
              val repeat = repeatText.toIntOrNull() ?: 1
              val increment = incrementText.toIntOrNull() ?: 1
              val outputStart = outputStartText.toInt()
              ranges += LineRange(outputStart, outputStart + repeat * increment - 1, fileId)
            }
          }
        }
        i++
      }
      return Smap(sourceClasses, ranges)
    }
  }
}
