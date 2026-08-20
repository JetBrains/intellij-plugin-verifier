/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.verifiers.method

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import kotlinx.metadata.KmClass
import kotlinx.metadata.jvm.KotlinClassMetadata
import kotlinx.metadata.jvm.Metadata
import kotlinx.metadata.jvm.getterSignature
import kotlinx.metadata.jvm.setterSignature
import kotlinx.metadata.jvm.signature
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.MethodInsnNode

object KotlinMethods {
  /**
   * Sized to survive concurrent verification: [isKotlinDefaultMethod] is asked about every method
   * by three verifiers, on as many threads as `intellij.plugin.verifier.concurrency.level` allows,
   * so a cache of a hundred booleans evicts an entry before its second reader arrives.
   */
  private const val CAPACITY = 16_384L

  private const val DEFAULT_IMPLS_SUFFIX = "\$DefaultImpls"

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
   * Neither signal is sufficient on its own, so this requires **both** to agree:
   *
   * 1. The class's own `@kotlin.Metadata` annotation (the compiler's structured record of what the
   *    *source* actually declared, via `kotlinx-metadata-jvm`) does not list this method. This is
   *    what rules out a developer-authored trivial override, which no bytecode shape can exclude.
   * 2. The method's body actually forwards to an interface's default implementation. This is what
   *    positively establishes that the method *is* a bridge, rather than merely being absent from
   *    the metadata.
   *
   * Requiring (2) matters because "absent from metadata" covers far more than bridges: metadata
   * tracks constructors and property accessors in [KmClass.constructors] and [KmClass.properties]
   * rather than [KmClass.functions], and synthetic `name$default` overloads are not listed at all.
   * Classifying any of those as a bridge would silently disable the argument-type, return-type and
   * local-variable checks that [MethodArgumentTypesVerifier], [MethodReturnTypeVerifier] and
   * [MethodLocalVarsVerifier] skip for default methods.
   */
  fun Method.isKotlinDefaultMethod(): Boolean {
    val method: Method = this
    return cache.get(method.location) { method.isCompilerSynthesizedInterfaceBridge() }
  }

  internal fun Method.isCompilerSynthesizedInterfaceBridge(): Boolean {
    // If this method doesn't have any bytecode (e.g. an abstract method), it can't be a bridge.
    if (instructions.isEmpty()) {
      return false
    }

    // A constructor or a static initializer is never an interface default method.
    if (isConstructor || isClassInitializer) {
      return false
    }

    // Evaluate the bytecode signal first: it is a single instruction scan, and it is false for
    // almost every method, whereas reading `@kotlin.Metadata` below deserializes the *whole*
    // class's metadata. That deserialization costs ~16 us per call, and every method of every
    // Kotlin class reaches this code (three verifiers ask about each method), so ordering the
    // cheap test first keeps metadata parsing to the handful of genuine bridge candidates.
    if (!forwardsToInterfaceDefaultImplementation()) {
      return false
    }

    val metadataAnnotation = containingClassFile.annotations
      .firstOrNull { it.desc == "Lkotlin/Metadata;" }
      ?: return false // filter non-Kotlin classes

    val kmClass = metadataAnnotation.toKmClassOrNull() ?: return false

    return !isDeclaredBy(kmClass)
  }

  /**
   * Whether the Kotlin source of this class declared this method itself, in any member position:
   * an ordinary function, a constructor, or a property accessor.
   */
  private fun Method.isDeclaredBy(kmClass: KmClass): Boolean {
    val declaredSignatures = kmClass.functions.mapNotNull { it.signature } +
      kmClass.constructors.mapNotNull { it.signature } +
      kmClass.properties.flatMap { listOfNotNull(it.getterSignature, it.setterSignature) }

    return declaredSignatures.any { it.name == name && it.descriptor == descriptor }
  }

  /**
   * Whether this method's body hands the call off to an interface's own default implementation of
   * the same method, which is what every Kotlin default-method lowering emits:
   *
   * * legacy and `-Xjvm-default=all-compatibility` lowering call the static holder,
   *   `INVOKESTATIC SomeInterface$DefaultImpls.method(...)`;
   * * newer compatibility lowering calls the interface method directly,
   *   `INVOKESPECIAL SomeInterface.method(...)`.
   *
   * Forwarding to anything else -- a method on this same class (`name$default` overloads) or an
   * `INVOKEINTERFACE` call through a delegate (`class Foo : Bar by delegate`) -- is not a default
   * method bridge.
   */
  private fun Method.forwardsToInterfaceDefaultImplementation(): Boolean =
    instructions.any { it is MethodInsnNode && it.forwardsTo(name) }

  private fun MethodInsnNode.forwardsTo(bridgedMethodName: String): Boolean {
    if (name != bridgedMethodName) {
      return false
    }
    return when (opcode) {
      Opcodes.INVOKESTATIC -> owner.endsWith(DEFAULT_IMPLS_SUFFIX)
      Opcodes.INVOKESPECIAL -> itf
      else -> false
    }
  }

  private fun AnnotationNode.toKmClassOrNull(): KmClass? {
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
