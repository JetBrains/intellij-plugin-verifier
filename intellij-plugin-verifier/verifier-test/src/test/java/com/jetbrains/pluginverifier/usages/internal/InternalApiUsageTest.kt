package com.jetbrains.pluginverifier.usages.internal

import com.jetbrains.plugin.structure.classes.resolvers.EMPTY_RESOLVER
import com.jetbrains.plugin.structure.classes.resolvers.FileOrigin
import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.modifiers.Modifiers
import com.jetbrains.pluginverifier.usages.internal.classDump.`TestInterface$DefaultImplsDump`
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileAsm
import org.junit.Assert.*
import org.junit.Test
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.ASM7
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.MethodNode

class InternalApiUsageTest {
  @Test
  fun `method annotated with @ApiStatus-Internal is detected as internal`() {
    val classFile = ClassFileAsm(mockClassNodeWithInternalMethod, origin)
    val internalApiMethod = classFile.methods.find { it.name == INTERNAL_API_METHOD_NAME }
    assertNotNull(internalApiMethod)
    internalApiMethod!!

    val usageLocation = createMethodLocation("SomeClass", "someMethod", "()V")
    assertTrue(internalApiMethod.isInternalApi(EMPTY_RESOLVER, usageLocation))
  }

  @Test
  fun `method not annotated with @ApiStatus-Internal is not detected as internal`() {
    val classFile = ClassFileAsm(mockClassNodeWithInternalMethod, origin)
    val regularMethod = classFile.methods.find { it.name == "handle" }
    assertNotNull(regularMethod)
    regularMethod!!

    val usageLocation = createMethodLocation("SomeClass", "someMethod", "()V")
    assertFalse(regularMethod.isInternalApi(EMPTY_RESOLVER, usageLocation))
  }

  @Test
  fun `kotlin legacy interface default method is not marked as internal`() {
    val origin = object : FileOrigin {
      override val parent = null
    }

    val classNode = ClassNode(ASM7)
    val classReader = ClassReader(`TestInterface$DefaultImplsDump`.dump())
    classReader.accept(classNode, 0)

    val classFile = ClassFileAsm(classNode, origin)
    val internalApiMethod = classFile.methods.find { it.name == INTERNAL_API_METHOD_NAME }
    assertNotNull(internalApiMethod)
    internalApiMethod!!

    val usageLocation = createMethodLocation("SomeClass", "someMethod", "()V")
    assertFalse(internalApiMethod.isInternalApi(EMPTY_RESOLVER, usageLocation))
  }

  // Helper to create a MethodLocation for testing
  private fun createMethodLocation(
    className: String,
    methodName: String,
    methodDescriptor: String
  ): MethodLocation {
    return MethodLocation(
      hostClass = ClassLocation(
        className = className,
        signature = null,
        modifiers = Modifiers.of(Modifiers.Modifier.PUBLIC),
        classFileOrigin = origin
      ),
      methodName = methodName,
      methodDescriptor = methodDescriptor,
      parameterNames = emptyList(),
      signature = null,
      modifiers = Modifiers.of(Modifiers.Modifier.PUBLIC)
    )
  }

  private val mockClassNodeWithInternalMethod: ClassNode
    get() = ClassNode(Opcodes.ASM9).apply {
      version = Opcodes.V1_8
      access = Opcodes.ACC_PUBLIC
      name = "com/jetbrains/Holder"
      superName = "java/lang/Object"

      // public void handle()
      val methodNode = MethodNode().apply {
        access = Opcodes.ACC_PUBLIC
        name = "handle"
        desc = "()V"
        instructions.add(InsnNode(Opcodes.RETURN))
      }
      methods.add(methodNode)

      // public void internalApiMethod() with @ApiStatus.Internal
      val internalApiMethod = MethodNode().apply {
        access = Opcodes.ACC_PUBLIC
        name = INTERNAL_API_METHOD_NAME
        desc = "()V"
        visibleAnnotations = listOf(
          AnnotationNode("L$INTERNAL_API_ANNOTATION_NAME;")
        )
        instructions.add(InsnNode(Opcodes.RETURN))
      }
      methods.add(internalApiMethod)
    }

  private val origin = object : FileOrigin {
    override val parent = null
  }
}

private const val INTERNAL_API_METHOD_NAME = "internalMethod"
private const val INTERNAL_API_ANNOTATION_NAME = "org/jetbrains/annotations/ApiStatus\$Internal"
