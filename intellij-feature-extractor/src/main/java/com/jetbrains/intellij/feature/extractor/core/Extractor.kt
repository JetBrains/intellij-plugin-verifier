package com.jetbrains.intellij.feature.extractor.core

import com.intellij.structure.resolvers.Resolver
import org.objectweb.asm.tree.ClassNode

abstract class Extractor(val resolver: Resolver) {

  data class Result(val extractedAll: Boolean, val featureNames: List<String>)

  fun extract(classNode: ClassNode): Result {
    val list: List<String>? = extractImpl(classNode)
    if (list == null) {
      LOG.info("Unable to extract all features of the plugin's implementor ${classNode.name.replace('/', '.')}")
      return Result(false, emptyList())
    }
    return Result(extractedAll, list)
  }

  /**
   * Whether all features of the plugin were successfully extracted.
   * If false, it's probably a tricky case which is not supported by the feature extractor.
   */
  protected var extractedAll: Boolean = false

  protected abstract fun extractImpl(classNode: ClassNode): List<String>?
}