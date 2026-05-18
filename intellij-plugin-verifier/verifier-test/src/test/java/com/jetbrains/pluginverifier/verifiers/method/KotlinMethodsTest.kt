/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.verifiers.method

import com.jetbrains.plugin.structure.classes.resolvers.FileOrigin
import com.jetbrains.pluginverifier.verifiers.method.KotlinMethods.isKotlinDefaultMethod
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileAsm
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.VarInsnNode

class KotlinMethodsTest {

  private val origin = object : FileOrigin {
    override val parent = null
  }

  /**
   * Simulates a Kotlin class that implements a Java interface with a default method.
   * The Kotlin compiler generates a stub method that delegates to the interface default
   * via INVOKESPECIAL:
   *
   * ```
   * ALOAD 0
   * INVOKESPECIAL com/example/MyInterface.getResult ()Lcom/example/InternalType;
   * ARETURN
   * ```
   */
  @Test
  fun `Java 8 default method delegation is detected as a Kotlin default method`() {
    val interfaceName = "com/example/java8/MyInterface"
    val methodName = "getResult"
    val methodDesc = "()Lcom/example/InternalType;"

    val classNode = createKotlinClassWithJava8DefaultDelegation(
      className = "com/example/java8/MyImpl",
      interfaceName = interfaceName,
      methodName = methodName,
      methodDesc = methodDesc
    )

    val classFile = ClassFileAsm(classNode, origin)
    val method = classFile.methods.find { it.name == methodName }!!

    assertTrue(
      "Method delegating to Java 8 interface default via INVOKESPECIAL should be detected",
      method.isKotlinDefaultMethod()
    )
  }

  /**
   * A method that does real work (not just delegation) should NOT be detected as a default method.
   */
  @Test
  fun `regular method is not detected as a default method`() {
    val classNode = ClassNode(Opcodes.ASM9).apply {
      version = Opcodes.V17
      access = Opcodes.ACC_PUBLIC
      name = "com/example/regular/MyClass"
      superName = "java/lang/Object"
      interfaces = listOf("com/example/regular/MyInterface")
      visibleAnnotations = listOf(kotlinMetadataAnnotation())

      methods.add(MethodNode().apply {
        access = Opcodes.ACC_PUBLIC
        name = "doWork"
        desc = "()V"
        // Just RETURN - trivial method but not delegating to interface
        instructions.add(InsnNode(Opcodes.RETURN))
      })
    }

    val classFile = ClassFileAsm(classNode, origin)
    val method = classFile.methods.find { it.name == "doWork" }!!

    assertFalse(
      "Regular method should not be detected as default method delegation",
      method.isKotlinDefaultMethod()
    )
  }

  /**
   * A class without Kotlin metadata should NOT have its methods detected as Kotlin default methods.
   */
  @Test
  fun `method in non-Kotlin class is not detected as default method`() {
    val interfaceName = "com/example/nonkotlin/MyInterface"
    val methodName = "getResult"
    val methodDesc = "()Lcom/example/InternalType;"

    // Same bytecode pattern but without Kotlin metadata
    val classNode = ClassNode(Opcodes.ASM9).apply {
      version = Opcodes.V17
      access = Opcodes.ACC_PUBLIC
      name = "com/example/nonkotlin/MyImpl"
      superName = "java/lang/Object"
      interfaces = listOf(interfaceName)
      // NO kotlin metadata annotation

      methods.add(MethodNode().apply {
        access = Opcodes.ACC_PUBLIC
        name = methodName
        desc = methodDesc
        instructions.add(VarInsnNode(Opcodes.ALOAD, 0))
        instructions.add(MethodInsnNode(
          Opcodes.INVOKESPECIAL,
          interfaceName,
          methodName,
          methodDesc,
          true
        ))
        instructions.add(InsnNode(Opcodes.ARETURN))
      })
    }

    val classFile = ClassFileAsm(classNode, origin)
    val method = classFile.methods.find { it.name == methodName }!!

    assertFalse(
      "Method in non-Kotlin class should not be detected as Kotlin default method",
      method.isKotlinDefaultMethod()
    )
  }

  /**
   * A method that delegates via INVOKESPECIAL to a class that is NOT a superinterface
   * should NOT be detected.
   */
  @Test
  fun `delegation to non-superinterface is not detected as default method`() {
    val methodName = "getResult"
    val methodDesc = "()Lcom/example/InternalType;"

    val classNode = ClassNode(Opcodes.ASM9).apply {
      version = Opcodes.V17
      access = Opcodes.ACC_PUBLIC
      name = "com/example/noniface/MyImpl"
      superName = "java/lang/Object"
      interfaces = listOf("com/example/noniface/SomeOtherInterface")  // different interface
      visibleAnnotations = listOf(kotlinMetadataAnnotation())

      methods.add(MethodNode().apply {
        access = Opcodes.ACC_PUBLIC
        name = methodName
        desc = methodDesc
        instructions.add(VarInsnNode(Opcodes.ALOAD, 0))
        instructions.add(MethodInsnNode(
          Opcodes.INVOKESPECIAL,
          "com/example/noniface/NotASuperInterface",  // not in the interfaces list
          methodName,
          methodDesc,
          true
        ))
        instructions.add(InsnNode(Opcodes.ARETURN))
      })
    }

    val classFile = ClassFileAsm(classNode, origin)
    val method = classFile.methods.find { it.name == methodName }!!

    assertFalse(
      "Delegation to non-superinterface should not be detected as default method",
      method.isKotlinDefaultMethod()
    )
  }

  /**
   * Java 8 default method delegation with parameters should also be detected.
   */
  @Test
  fun `Java 8 default method delegation with parameters is detected`() {
    val interfaceName = "com/example/params/MyInterface"
    val methodName = "process"
    val methodDesc = "(Ljava/lang/String;I)Lcom/example/Result;"

    val classNode = ClassNode(Opcodes.ASM9).apply {
      version = Opcodes.V17
      access = Opcodes.ACC_PUBLIC
      name = "com/example/params/MyImpl"
      superName = "java/lang/Object"
      interfaces = listOf(interfaceName)
      visibleAnnotations = listOf(kotlinMetadataAnnotation())

      methods.add(MethodNode().apply {
        access = Opcodes.ACC_PUBLIC
        name = methodName
        desc = methodDesc
        // ALOAD 0 (this)
        instructions.add(VarInsnNode(Opcodes.ALOAD, 0))
        // ALOAD 1 (String param)
        instructions.add(VarInsnNode(Opcodes.ALOAD, 1))
        // ILOAD 2 (int param)
        instructions.add(VarInsnNode(Opcodes.ILOAD, 2))
        // INVOKESPECIAL
        instructions.add(MethodInsnNode(
          Opcodes.INVOKESPECIAL,
          interfaceName,
          methodName,
          methodDesc,
          true
        ))
        instructions.add(InsnNode(Opcodes.ARETURN))
      })
    }

    val classFile = ClassFileAsm(classNode, origin)
    val method = classFile.methods.find { it.name == methodName }!!

    assertTrue(
      "Java 8 default method delegation with parameters should be detected",
      method.isKotlinDefaultMethod()
    )
  }

  /**
   * A void-returning Java 8 default method delegation should be detected.
   */
  @Test
  fun `Java 8 default method delegation with void return is detected`() {
    val interfaceName = "com/example/voidreturn/MyInterface"
    val methodName = "doSomething"
    val methodDesc = "()V"

    val classNode = createKotlinClassWithJava8DefaultDelegation(
      className = "com/example/voidreturn/MyImpl",
      interfaceName = interfaceName,
      methodName = methodName,
      methodDesc = methodDesc,
      returnOpcode = Opcodes.RETURN
    )

    val classFile = ClassFileAsm(classNode, origin)
    val method = classFile.methods.find { it.name == methodName }!!

    assertTrue(
      "Void-returning Java 8 default method delegation should be detected",
      method.isKotlinDefaultMethod()
    )
  }

  /**
   * When a Kotlin class overrides an interface method with a covariant return type,
   * the compiler generates a synthetic bridge method with the original return type.
   * This bridge method should NOT be flagged by MethodReturnTypeVerifier.
   */
  @Test
  fun `bridge method generated for covariant return type is skipped by MethodReturnTypeVerifier`() {
    val interfaceName = "com/example/bridge/MyInterface"
    val methodName = "getCollector"
    val bridgeDesc = "()Lcom/example/bridge/InternalType;"

    val classNode = ClassNode(Opcodes.ASM9).apply {
      version = Opcodes.V17
      access = Opcodes.ACC_PUBLIC
      name = "com/example/bridge/MyImpl"
      superName = "java/lang/Object"
      interfaces = listOf(interfaceName)
      visibleAnnotations = listOf(kotlinMetadataAnnotation())

      // The bridge method: ACC_PUBLIC | ACC_BRIDGE | ACC_SYNTHETIC
      methods.add(MethodNode().apply {
        access = Opcodes.ACC_PUBLIC or Opcodes.ACC_BRIDGE or Opcodes.ACC_SYNTHETIC
        name = methodName
        desc = bridgeDesc
        instructions.add(VarInsnNode(Opcodes.ALOAD, 0))
        instructions.add(MethodInsnNode(
          Opcodes.INVOKEVIRTUAL,
          "com/example/bridge/MyImpl",
          methodName,
          "()Lcom/example/bridge/PublicSubtype;",
          false
        ))
        instructions.add(InsnNode(Opcodes.ARETURN))
      })
    }

    val classFile = ClassFileAsm(classNode, origin)
    val bridgeMethod = classFile.methods.find { it.name == methodName }!!

    assertTrue(
      "Bridge method should be detected as bridge",
      bridgeMethod.isBridgeMethod
    )
  }

  private fun createKotlinClassWithJava8DefaultDelegation(
    className: String,
    interfaceName: String,
    methodName: String,
    methodDesc: String,
    returnOpcode: Int = Opcodes.ARETURN
  ): ClassNode {
    return ClassNode(Opcodes.ASM9).apply {
      version = Opcodes.V17
      access = Opcodes.ACC_PUBLIC
      name = className
      superName = "java/lang/Object"
      interfaces = listOf(interfaceName)
      visibleAnnotations = listOf(kotlinMetadataAnnotation())

      methods.add(MethodNode().apply {
        access = Opcodes.ACC_PUBLIC
        this.name = methodName
        desc = methodDesc
        instructions.add(VarInsnNode(Opcodes.ALOAD, 0))
        instructions.add(MethodInsnNode(
          Opcodes.INVOKESPECIAL,
          interfaceName,
          methodName,
          methodDesc,
          true
        ))
        instructions.add(InsnNode(returnOpcode))
      })
    }
  }

  private fun kotlinMetadataAnnotation(): org.objectweb.asm.tree.AnnotationNode {
    return org.objectweb.asm.tree.AnnotationNode("Lkotlin/Metadata;").apply {
      values = listOf("mv", intArrayOf(2, 0, 0), "k", 1, "xi", 48)
    }
  }
}
