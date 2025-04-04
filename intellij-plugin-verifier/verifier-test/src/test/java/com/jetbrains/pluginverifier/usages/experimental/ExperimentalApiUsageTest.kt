package com.jetbrains.pluginverifier.usages.experimental

import com.jetbrains.plugin.structure.classes.resolvers.EMPTY_RESOLVER
import com.jetbrains.plugin.structure.classes.resolvers.FileOrigin
import com.jetbrains.pluginverifier.usages.util.MemberAnnotation
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileAsm
import org.junit.Assert.*
import org.junit.Test
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.MethodNode

class ExperimentalApiUsageTest {
  @Test
  fun `method annotated with @ApiStatus-Experimental is detected as experimental`() {
    val classFile = ClassFileAsm(mockClassNode, origin)
    val experimentalApiMethod = classFile.methods.find { it.name == EXPERIMENTAL_API_METHOD_NAME }
    assertNotNull(experimentalApiMethod)
    experimentalApiMethod!!

    assertTrue(experimentalApiMethod.isExperimentalApi(EMPTY_RESOLVER))
  }

  @Test
  fun `method annotated with @ApiStatus-Experimental is resolved as direct annotation`() {
    val classFile = ClassFileAsm(mockClassNode, origin)
    val experimentalApiMethod = classFile.methods.find { it.name == EXPERIMENTAL_API_METHOD_NAME }
    assertNotNull(experimentalApiMethod)
    experimentalApiMethod!!

    val annotation = experimentalApiMethod.resolveExperimentalApiAnnotation(EMPTY_RESOLVER)
    assertNotNull(annotation)
    annotation!!
    val expectedAnnotation =
      MemberAnnotation.AnnotatedDirectly(experimentalApiMethod, EXPERIMENTAL_API_ANNOTATION_NAME)

    assertEquals(expectedAnnotation.annotationName, annotation.annotationName)
    assertEquals(expectedAnnotation.member, annotation.member)
  }

  private val mockClassNode: ClassNode
    get() = ClassNode(Opcodes.ASM9).apply {
      version = Opcodes.V1_8 // Java 8
      access = Opcodes.ACC_PUBLIC
      name = "com/jetbrains/Holder"
      superName = "java/lang/Object"

      // public void handle()
      val methodNode = MethodNode().apply {
        access = Opcodes.ACC_PUBLIC
        name = "handle"
        desc = "()V"
        // return;
        instructions.add(InsnNode(Opcodes.RETURN))
      }
      methods.add(methodNode)

      // public void experimentalApiMethod()
      val experimentalApiMethod = MethodNode().apply {
        access = Opcodes.ACC_PUBLIC
        name = EXPERIMENTAL_API_METHOD_NAME
        desc = "()V"
        visibleAnnotations = listOf(
          AnnotationNode("L$EXPERIMENTAL_API_ANNOTATION_NAME;")
        )
        // return;
        instructions.add(InsnNode(Opcodes.RETURN))
      }
      methods.add(experimentalApiMethod)
    }

  private val origin = object : FileOrigin {
    override val parent = null
  }
}

private const val EXPERIMENTAL_API_METHOD_NAME = "experimentalApiMethod"
private const val EXPERIMENTAL_API_ANNOTATION_NAME = "org/jetbrains/annotations/ApiStatus\$Experimental"

