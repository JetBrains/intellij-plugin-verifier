/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.jetbrains.pluginverifier.usages.properties

import com.jetbrains.plugin.structure.classes.resolvers.FileOrigin
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.problems.MissingPropertyReferenceProblem
import com.jetbrains.pluginverifier.results.reference.MethodReference
import com.jetbrains.pluginverifier.tests.mocks.MockVerificationContext
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileAsm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*

class EnumClassPropertyUsageAdapterTest {
  private val missingPropertyKey = "property.key"
  private val resourceBundleName = "messages.Messages"

  @Test
  fun `EnumClassPropertyUsageAdapter should support init method`() {
    val enumClassPropertyUsageAdapter = EnumClassPropertyUsageAdapter()

    val classFile = ClassFileAsm(
      generateJavaEnumClass(
        listOf(Pair("Ljava/lang/String;", { it.add(LdcInsnNode(missingPropertyKey)) })),
        addAnnotation = true
      ), origin
    )
    val initMethod = classFile.methods.find { it.name == "<init>" }!!

    assertTrue(enumClassPropertyUsageAdapter.supports(initMethod))
  }

  @Test
  fun `isEnumConstructorDesc should match any enum constructor with a single String param`() {
    val enumClassPropertyUsageAdapter = EnumClassPropertyUsageAdapter()

    assertTrue(enumClassPropertyUsageAdapter.isEnumConstructorDesc("(Ljava/lang/String;ILjava/lang/String;)V"))
    assertTrue(enumClassPropertyUsageAdapter.isEnumConstructorDesc("(Ljava/lang/String;IILjava/lang/String;)V"))
    assertTrue(enumClassPropertyUsageAdapter.isEnumConstructorDesc("(Ljava/lang/String;ILjava/lang/Object;Ljava/lang/String;)V"))
    assertFalse(enumClassPropertyUsageAdapter.isEnumConstructorDesc("(Ljava/lang/String;I)V"))
    assertFalse(enumClassPropertyUsageAdapter.isEnumConstructorDesc("(Ljava/lang/String;)V"))
  }

  @Test
  fun `missing property should be reported for enum class constructor parameter with @PropertyKey annotation`() {
    val enumPropertyUsageProcessor = EnumPropertyUsageProcessor(alwaysFailingPropertyChecker)

    val classFile = ClassFileAsm(
      generateJavaEnumClass(
        listOf(Pair("Ljava/lang/String;", { it.add(LdcInsnNode(missingPropertyKey)) })),
        addAnnotation = true
      ), origin
    )
    val initMethod = classFile.methods.find { it.name == "<init>" }!!
    val clinitMethod = classFile.methods.find { it.name == "<clinit>" }!!

    val context = MockVerificationContext()
    enumPropertyUsageProcessor.processMethodInvocation(
      MethodReference(classFile.name, initMethod.name, initMethod.descriptor),
      initMethod,
      MethodInsnNode(Opcodes.RETURN, classFile.name, initMethod.name, initMethod.descriptor),
      clinitMethod, context
    )

    (context.problems.first() as MissingPropertyReferenceProblem).apply {
      assertEquals(missingPropertyKey, propertyKey)
      assertEquals(resourceBundleName, bundleBaseName)
      assertEquals("<clinit>", (usageLocation as MethodLocation).methodName)
      assertEquals("com/jetbrains/JavaEnumExample", (usageLocation as MethodLocation).containingClass.className)
      assertEquals("Missing property reference", problemType)
    }
  }

  @Test
  fun `no error should be reported for enum class constructor parameter without annotation`() {
    val enumPropertyUsageProcessor = EnumPropertyUsageProcessor(alwaysFailingPropertyChecker)

    val classFile = ClassFileAsm(
      generateJavaEnumClass(listOf(), addAnnotation = false), origin
    )
    val initMethod = classFile.methods.find { it.name == "<init>" }!!
    val clinitMethod = classFile.methods.find { it.name == "<clinit>" }!!

    val context = MockVerificationContext()
    enumPropertyUsageProcessor.processMethodInvocation(
      MethodReference(classFile.name, initMethod.name, initMethod.descriptor),
      initMethod,
      MethodInsnNode(Opcodes.RETURN, classFile.name, initMethod.name, initMethod.descriptor),
      clinitMethod, context
    )

    assertTrue(context.problems.isEmpty())
  }

  /**
   * Generates a class file similar to a Java enum.
   * @param additionalInitParams Additional parameters for the enum's constructor besides the 2 synthetic ones.
   *  The String is the type of the parameter in JVM Descriptor form, while the lambda can be used to add
   *  additional initializing logic to the <clinit> function (e.g. loading a constant as an argument for our parameter)
   * @param addAnnotation Whether to add a @PropertyKey annotation to the list of RuntimeInvisibleParameterAnnotations.
   *  Currently, it will simply decide whether to add one at index 0.
   */
  private fun generateJavaEnumClass(additionalInitParams: List<Pair<String, (InsnList) -> Unit>> = listOf(), addAnnotation: Boolean): ClassNode {
    val className = "com/jetbrains/JavaEnumExample"

    return ClassNode(Opcodes.ASM9).apply {
      version = Opcodes.V1_8 // Java 8
      access = Opcodes.ACC_FINAL or Opcodes.ACC_ENUM or Opcodes.ACC_SUPER
      name = className
      superName = "java/lang/Enum"

      val descriptor = "(Ljava/lang/String;I${additionalInitParams.joinToString { it.first }})V"

      val initMethodNode = MethodNode().apply {
        access = Opcodes.ACC_PRIVATE
        name = "<init>"
        desc = descriptor

        if (addAnnotation) {
          invisibleParameterAnnotations = arrayOf(
            listOf(
              AnnotationNode("Lorg/jetbrains/annotations/PropertyKey;").apply { values = listOf("resourceBundle", resourceBundleName) }
            ))
        }

        // return;
        instructions.add(InsnNode(Opcodes.RETURN))
      }
      val clinitMethodNode = MethodNode().apply {
        access = Opcodes.ACC_STATIC
        name = "<clinit>"
        desc = "()V"

        tryCatchBlocks = listOf()

        // Dummy clinit function
        // It creates the enum constant `ENUM_NAME` with ordinal 0. Its first non-synthetic parameter is a property key.
        instructions.add(TypeInsnNode(Opcodes.NEW, className))
        instructions.add(LdcInsnNode("ENUM_NAME"))
        instructions.add(LdcInsnNode(0))
        additionalInitParams.forEach {
          it.second(instructions)
        }
        instructions.add(MethodInsnNode(Opcodes.INVOKESPECIAL, className, "<init>", descriptor))
        instructions.add(InsnNode(Opcodes.RETURN))

        maxStack = 4
      }

      methods.add(initMethodNode)
      methods.add(clinitMethodNode)
    }
  }

  private val alwaysFailingPropertyChecker = object : PropertyChecker {
    override fun checkProperty(resourceBundleName: String, propertyKey: String, context: VerificationContext, usageLocation: Location) {
      // Mock for always registering a missing property reference
      context.problemRegistrar.registerProblem(
        MissingPropertyReferenceProblem(
          propertyKey,
          resourceBundleName,
          usageLocation
        )
      )
    }
  }


  private val origin = object : FileOrigin {
    override val parent = null
  }

}
