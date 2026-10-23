/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.usages.deprecated

import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.tests.BaseBytecodeTest
import com.jetbrains.pluginverifier.tests.VerificationRunner
import com.jetbrains.pluginverifier.tests.mocks.IdeaPluginSpec
import kotlinx.metadata.KmClassifier
import kotlinx.metadata.KmFunction
import kotlinx.metadata.KmType
import kotlinx.metadata.jvm.JvmMethodSignature
import kotlinx.metadata.jvm.signature
import org.junit.Assert.assertEquals
import org.junit.Test
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.VarInsnNode

/**
 * Verifies a plugin against a deprecated Kotlin interface default method `Api.foo()` compiled with
 * `-jvm-default=disable`, which the `mock-plugin` fixtures, compiled by this project's toolchain,
 * cannot produce. The classes model what kotlinc emits for:
 *
 * ```
 * // IDE
 * interface Api {
 *   @Deprecated("...") fun foo(): String = "default"
 * }
 *
 * // plugin
 * class Bridge : Api
 * class Override : Api {
 *   override fun foo(): String = super.foo()
 * }
 * ```
 *
 * plus a Java class overriding `foo()` with an explicit `Api.DefaultImpls.foo(this)` call. Like older
 * Kotlin compilers did, `Api$DefaultImpls.foo` is deprecated as well.
 *
 * A plugin interface `SubApi : Api` is not covered: whichever mode the plugin is compiled with, it
 * gets a forwarding `SubApi$DefaultImpls.foo` whose call to the deprecated `Api$DefaultImpls.foo`
 * is still reported, see `KotlinMethods.isKotlinDefaultMethod`.
 */
class DeprecatedKotlinDefaultMethodTest : BaseBytecodeTest() {

  @Test
  fun `only source-declared overrides of a legacy deprecated default method are reported`() {
    val ide = buildIdeWithBundledPlugins {
      dir(IDE_PACKAGE) {
        file("Api.class", api())
        file("Api\$DefaultImpls.class", apiDefaultImpls())
      }
    }
    val plugin = prepareUsage(
      IdeaPluginSpec("com.example.legacyKotlinDefaults", "Some Vendor"),
      PLUGIN_PACKAGE,
      mapOf(
        "Bridge" to kotlinClass("Bridge", API, foo(forwardingTo = DEFAULT_IMPLS), declaresFoo = false),
        "Override" to kotlinClass("Override", API, foo(forwardingTo = DEFAULT_IMPLS), declaresFoo = true),
        "JavaOverride" to javaClass("JavaOverride", API, foo(forwardingTo = DEFAULT_IMPLS)),
      )
    )

    val result = VerificationRunner().runPluginVerification(ide, plugin) as PluginVerificationResult.Verified

    assertEquals(emptySet<CompatibilityProblem>(), result.compatibilityProblems)
    assertEquals(
      setOf(
        "DeprecatedMethodOverridden in $PLUGIN_PACKAGE/Override",
        "DeprecatedMethodUsage in $PLUGIN_PACKAGE/Override",
        "DeprecatedMethodOverridden in $PLUGIN_PACKAGE/JavaOverride",
        "DeprecatedMethodUsage in $PLUGIN_PACKAGE/JavaOverride",
      ),
      result.deprecatedUsages.mapTo(hashSetOf()) { "${it.javaClass.simpleName} in ${it.usageLocation.containingClass.className}" }
    )
  }

  private fun api(): ByteArray = classBytes(API, Opcodes.ACC_PUBLIC or Opcodes.ACC_INTERFACE or Opcodes.ACC_ABSTRACT) {
    methods.add(deprecated(MethodNode(Opcodes.ACC_PUBLIC or Opcodes.ACC_ABSTRACT, "foo", "()$STRING", null, null)))
  }

  private fun apiDefaultImpls(): ByteArray = classBytes(DEFAULT_IMPLS, Opcodes.ACC_PUBLIC or Opcodes.ACC_FINAL or Opcodes.ACC_SUPER) {
    methods.add(deprecated(MethodNode(Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC, "foo", "(L$API;)$STRING", null, null)).apply {
      instructions.add(LdcInsnNode("default"))
      instructions.add(InsnNode(Opcodes.ARETURN))
    })
  }

  // `public String foo() { return Owner.DefaultImpls.foo(this); }`
  private fun foo(forwardingTo: String): MethodNode =
    MethodNode(Opcodes.ACC_PUBLIC, "foo", "()$STRING", null, null).apply {
      val ownerInterface = forwardingTo.removeSuffix("\$DefaultImpls")
      instructions.add(VarInsnNode(Opcodes.ALOAD, 0))
      instructions.add(MethodInsnNode(Opcodes.INVOKESTATIC, forwardingTo, "foo", "(L$ownerInterface;)$STRING", false))
      instructions.add(InsnNode(Opcodes.ARETURN))
    }

  private fun kotlinClass(simpleName: String, implementedInterface: String, foo: MethodNode, declaresFoo: Boolean): ByteArray {
    val className = "$PLUGIN_PACKAGE/$simpleName"
    val declaredFunctions = if (declaresFoo) listOf(JvmMethodSignature("foo", "()$STRING")) else emptyList()
    return classBytes(className, Opcodes.ACC_PUBLIC or Opcodes.ACC_FINAL or Opcodes.ACC_SUPER) {
      interfaces = mutableListOf(implementedInterface)
      visibleAnnotations = mutableListOf(kotlinMetadataAnnotation(className, declaredFunctions))
      methods.add(constructor())
      methods.add(foo)
    }
  }

  private fun javaClass(simpleName: String, implementedInterface: String, foo: MethodNode): ByteArray =
    classBytes("$PLUGIN_PACKAGE/$simpleName", Opcodes.ACC_PUBLIC or Opcodes.ACC_SUPER) {
      interfaces = mutableListOf(implementedInterface)
      methods.add(constructor())
      methods.add(foo)
    }

  private fun constructor(): MethodNode = MethodNode(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null).apply {
    instructions.add(VarInsnNode(Opcodes.ALOAD, 0))
    instructions.add(MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false))
    instructions.add(InsnNode(Opcodes.RETURN))
  }

  private fun deprecated(methodNode: MethodNode) = methodNode.apply {
    access = access or Opcodes.ACC_DEPRECATED
    visibleAnnotations = mutableListOf(AnnotationNode("Ljava/lang/Deprecated;"))
  }

  private fun classBytes(className: String, classAccess: Int, configure: ClassNode.() -> Unit): ByteArray {
    val classNode = ClassNode(Opcodes.ASM9).apply {
      version = Opcodes.V1_8
      access = classAccess
      name = className
      superName = "java/lang/Object"
      configure()
    }
    return ClassWriter(ClassWriter.COMPUTE_MAXS).also { classNode.accept(it) }.toByteArray()
  }

  /**
   * The `@kotlin.Metadata` of a Kotlin class or interface that lists exactly [declaredFunctions],
   * i.e. the functions its source declares.
   */
  private fun kotlinMetadataAnnotation(className: String, declaredFunctions: List<JvmMethodSignature>): AnnotationNode {
    val metadata = kotlinMetadata {
      name = className
      functions += declaredFunctions.map { signature ->
        KmFunction(signature.name).apply {
          this.signature = signature
          returnType = KmType().apply { classifier = KmClassifier.Class("kotlin/String") }
        }
      }
    }
    return AnnotationNode("Lkotlin/Metadata;").apply {
      values = mutableListOf(
        "k", metadata.kind,
        "mv", metadata.metadataVersion.toList(),
        "d1", metadata.data1.toList(),
        "d2", metadata.data2.toList()
      )
    }
  }

  private companion object {
    const val STRING = "Ljava/lang/String;"
    const val IDE_PACKAGE = "legacyKotlinApi"
    const val PLUGIN_PACKAGE = "legacyKotlinPlugin"
    const val API = "$IDE_PACKAGE/Api"
    const val DEFAULT_IMPLS = "$API\$DefaultImpls"
  }
}
