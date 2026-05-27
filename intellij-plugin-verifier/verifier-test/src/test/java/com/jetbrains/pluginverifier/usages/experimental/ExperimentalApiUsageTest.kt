package com.jetbrains.pluginverifier.usages.experimental

import com.jetbrains.plugin.structure.classes.resolvers.EMPTY_RESOLVER
import com.jetbrains.plugin.structure.classes.resolvers.FileOrigin
import com.jetbrains.plugin.structure.classes.resolvers.FixedClassesResolver
import com.jetbrains.pluginverifier.usages.util.MemberAnnotation
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileAsm
import org.junit.Assert.*
import org.junit.Test
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
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

  @Test
  fun `Companion field is detected as experimental when companion object is annotated`() {
    val resolver = FixedClassesResolver.create(
      listOf(mockClassNodeWithCompanionField, mockExperimentalCompanionClassNode),
      origin
    )
    val classFile = ClassFileAsm(mockClassNodeWithCompanionField, origin)
    val companionField = classFile.fields.find { it.name == "Companion" }
    assertNotNull(companionField)
    companionField!!

    assertTrue(companionField.isExperimentalApi(resolver))
  }

  @Test
  fun `JvmStatic method is detected as experimental when companion object is annotated`() {
    val resolver = FixedClassesResolver.create(
      listOf(mockClassNodeWithJvmStaticMethod, mockExperimentalCompanionClassNode),
      origin
    )
    val classFile = ClassFileAsm(mockClassNodeWithJvmStaticMethod, origin)
    val jvmStaticMethod = classFile.methods.find { it.name == JVM_STATIC_METHOD_NAME }
    assertNotNull(jvmStaticMethod)
    jvmStaticMethod!!

    assertTrue(jvmStaticMethod.isExperimentalApi(resolver))
  }

  @Test
  fun `JvmStatic method is detected as experimental when companion method is annotated`() {
    val resolver = FixedClassesResolver.create(
      listOf(mockClassNodeWithJvmStaticMethod, mockCompanionClassNodeWithExperimentalMethod),
      origin
    )
    val classFile = ClassFileAsm(mockClassNodeWithJvmStaticMethod, origin)
    val jvmStaticMethod = classFile.methods.find { it.name == JVM_STATIC_METHOD_NAME }
    assertNotNull(jvmStaticMethod)
    jvmStaticMethod!!

    assertTrue(jvmStaticMethod.isExperimentalApi(resolver))
  }

  @Test
  fun `JvmStatic default method is detected as experimental when companion object is annotated`() {
    val resolver = FixedClassesResolver.create(
      listOf(mockClassNodeWithJvmStaticDefaultMethod, mockExperimentalCompanionClassNodeWithDefaultMethod),
      origin
    )
    val classFile = ClassFileAsm(mockClassNodeWithJvmStaticDefaultMethod, origin)
    val jvmStaticDefaultMethod = classFile.methods.find { it.name == JVM_STATIC_DEFAULT_METHOD_NAME }
    assertNotNull(jvmStaticDefaultMethod)
    jvmStaticDefaultMethod!!

    assertTrue(jvmStaticDefaultMethod.isExperimentalApi(resolver))
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

  private val mockClassNodeWithCompanionField: ClassNode
    get() = ClassNode(Opcodes.ASM9).apply {
      version = Opcodes.V1_8
      access = Opcodes.ACC_PUBLIC
      name = HOLDER_CLASS_NAME
      superName = "java/lang/Object"

      fields.add(
        FieldNode(
          Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC or Opcodes.ACC_FINAL,
          "Companion",
          "L$COMPANION_CLASS_NAME;",
          null,
          null
        )
      )
    }

  private val mockClassNodeWithJvmStaticMethod: ClassNode
    get() = ClassNode(Opcodes.ASM9).apply {
      version = Opcodes.V1_8
      access = Opcodes.ACC_PUBLIC
      name = HOLDER_CLASS_NAME
      superName = "java/lang/Object"

      methods.add(staticMethod(JVM_STATIC_METHOD_NAME, "()V"))
    }

  private val mockClassNodeWithJvmStaticDefaultMethod: ClassNode
    get() = ClassNode(Opcodes.ASM9).apply {
      version = Opcodes.V1_8
      access = Opcodes.ACC_PUBLIC
      name = HOLDER_CLASS_NAME
      superName = "java/lang/Object"

      methods.add(staticMethod(JVM_STATIC_DEFAULT_METHOD_NAME, "(ILjava/lang/Object;)V"))
    }

  private val mockExperimentalCompanionClassNode: ClassNode
    get() = companionClassNode(
      annotations = listOf(AnnotationNode("L$EXPERIMENTAL_API_ANNOTATION_NAME;")),
      methods = listOf(instanceMethod(JVM_STATIC_METHOD_NAME, "()V"))
    )

  private val mockCompanionClassNodeWithExperimentalMethod: ClassNode
    get() = companionClassNode(
      methods = listOf(
        instanceMethod(
          JVM_STATIC_METHOD_NAME,
          "()V",
          annotations = listOf(AnnotationNode("L$EXPERIMENTAL_API_ANNOTATION_NAME;"))
        )
      )
    )

  private val mockExperimentalCompanionClassNodeWithDefaultMethod: ClassNode
    get() = companionClassNode(
      annotations = listOf(AnnotationNode("L$EXPERIMENTAL_API_ANNOTATION_NAME;")),
      methods = listOf(staticMethod(JVM_STATIC_DEFAULT_METHOD_NAME, "(L$COMPANION_CLASS_NAME;ILjava/lang/Object;)V"))
    )

  private fun companionClassNode(
    annotations: List<AnnotationNode> = emptyList(),
    methods: List<MethodNode> = emptyList()
  ): ClassNode = ClassNode(Opcodes.ASM9).apply {
    version = Opcodes.V1_8
    access = Opcodes.ACC_PUBLIC or Opcodes.ACC_FINAL
    name = COMPANION_CLASS_NAME
    superName = "java/lang/Object"
    outerClass = HOLDER_CLASS_NAME
    visibleAnnotations = annotations
    this.methods.addAll(methods)
  }

  private fun staticMethod(name: String, descriptor: String): MethodNode =
    MethodNode(Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC, name, descriptor, null, null).apply {
      instructions.add(InsnNode(Opcodes.RETURN))
    }

  private fun instanceMethod(
    name: String,
    descriptor: String,
    annotations: List<AnnotationNode> = emptyList()
  ): MethodNode =
    MethodNode(Opcodes.ACC_PUBLIC or Opcodes.ACC_FINAL, name, descriptor, null, null).apply {
      visibleAnnotations = annotations
      instructions.add(InsnNode(Opcodes.RETURN))
    }

  private val origin = object : FileOrigin {
    override val parent = null
  }
}

private const val EXPERIMENTAL_API_METHOD_NAME = "experimentalApiMethod"
private const val EXPERIMENTAL_API_ANNOTATION_NAME = "org/jetbrains/annotations/ApiStatus\$Experimental"
private const val HOLDER_CLASS_NAME = "com/jetbrains/Holder"
private const val COMPANION_CLASS_NAME = "$HOLDER_CLASS_NAME\$Companion"
private const val JVM_STATIC_METHOD_NAME = "jvmStaticMethod"
private const val JVM_STATIC_DEFAULT_METHOD_NAME = "jvmStaticMethod\$default"
