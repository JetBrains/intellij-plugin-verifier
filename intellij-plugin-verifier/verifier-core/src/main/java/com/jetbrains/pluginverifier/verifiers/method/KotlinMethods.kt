/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.verifiers.method

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import kotlinx.metadata.jvm.KotlinClassMetadata
import kotlinx.metadata.jvm.Metadata
import kotlinx.metadata.jvm.signature
import org.objectweb.asm.tree.AnnotationNode

object KotlinMethods {
  private const val CAPACITY = 100L

  private val cache: Cache<MethodLocation, Boolean> = Caffeine.newBuilder()
    .maximumSize(CAPACITY)
    .build()

  /**
   * Identify a Kotlin default method.
   *
   * A kotlin default method is a method that has a default implementation, but Kotlin still emits
   * a concrete method body for it in every implementing class that doesn't override it -- the JVM
   * requires *some* body to exist, so the compiler synthesizes a bridge that forwards to the
   * interface's real implementation. That bridge's bytecode references whatever types the default
   * method's signature uses, so a naive bytecode scan would misattribute those references to the
   * (non-existent) developer code in the implementing class.
   *
   * This used to be detected by pattern-matching the bridge's bytecode shape, but that shape is a
   * Kotlin-compiler implementation detail that has changed across versions (e.g. Kotlin 2.x's
   * `-Xjvm-default=all-compatibility` switched from an `INVOKESTATIC` call into a `$DefaultImpls`
   * inner class to an `INVOKESPECIAL` call directly on the interface's real default method) --
   * and, worse, a developer-authored trivial override (`override fun foo() = super.foo()`) can
   * compile to the exact same instructions as the synthesized bridge, so no bytecode shape can
   * distinguish the two cases in general.
   *
   * Instead, this reads the class's own `@kotlin.Metadata` annotation (the compiler's structured
   * record of what the *source* actually declared, via `kotlinx-metadata-jvm`) and checks whether
   * this method is present in the class's own declared functions. If it isn't, the compiler must
   * have synthesized it -- regardless of which lowering strategy produced its bytecode.
   */
  fun Method.isKotlinDefaultMethod(): Boolean {
    val method: Method = this
    return cache.get(method.location) { method.isCompilerSynthesizedInterfaceBridge() }
  }

  private fun Method.isCompilerSynthesizedInterfaceBridge(): Boolean {
    // If this method doesn't have any bytecode (e.g. an abstract method), it can't be a bridge.
    if (instructions.isEmpty()) {
      return false
    }

    val metadataAnnotation = containingClassFile.annotations
      .firstOrNull { it.desc == "Lkotlin/Metadata;" }
      ?: return false // filter non-Kotlin classes

    val kmClass = metadataAnnotation.toKmClassOrNull() ?: return false

    val isDeclaredByThisClass = kmClass.functions.any { function ->
      val signature = function.signature
      signature != null && signature.name == name && signature.descriptor == descriptor
    }

    // Kotlin's metadata only lists functions the source actually declared in this class. If this
    // method isn't among them, it wasn't written by a developer -- the compiler synthesized it,
    // most commonly as a compatibility bridge for an inherited-but-unoverridden default method.
    return !isDeclaredByThisClass
  }

  private fun AnnotationNode.toKmClassOrNull(): kotlinx.metadata.KmClass? {
    val metadata = toMetadata()
    if (metadata.metadataVersion.isEmpty()) {
      return null
    }
    return try {
      (KotlinClassMetadata.readLenient(metadata) as? KotlinClassMetadata.Class)?.kmClass
    } catch (e: IllegalArgumentException) {
      null
    }
  }

  private fun AnnotationNode.toMetadata() = Metadata(
    kind = int("k"),
    metadataVersion = ints("mv"),
    data1 = strings("d1"),
    data2 = strings("d2"))

  private fun AnnotationNode.int(key: String) = get<Int>(key)
  private fun AnnotationNode.ints(key: String) = get<List<Int>?>(key)?.toIntArray()
  private fun AnnotationNode.strings(key: String) = get<List<String>?>(key)?.toTypedArray()

  private inline fun <reified T> AnnotationNode.get(key: String): T? {
    return getAnnotationValue(key)?.let { value ->
      if (value is T) value else null
    }
  }

  private fun AnnotationNode.getAnnotationValue(key: String): Any? {
    val values = values ?: return null
    for (i in 0 until values.size step 2) {
      if (values[i] == key) {
        return values[i + 1]
      }
    }
    return null
  }
}
