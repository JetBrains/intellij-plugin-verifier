package com.jetbrains.intellij.feature.extractor.core

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import org.objectweb.asm.tree.ClassNode

abstract class Extractor(val resolver: Resolver) {

  data class Result(val extractedAll: Boolean, val featureNames: List<String>)

  fun extract(classNode: ClassNode): Result {
    val featureNames = extractImpl(classNode) ?: return Result(false, emptyList())
    return Result(extractedAll, featureNames)
  }

  /**
   * Whether all features of the plugin were successfully extracted.
   * If false, it's probably a tricky case which is not supported by the feature extractor.
   */
  protected var extractedAll: Boolean = false

  protected abstract fun extractImpl(classNode: ClassNode): List<String>?
}