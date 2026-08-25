package com.jetbrains.pluginverifier.verifiers.resolution

import com.jetbrains.plugin.structure.classes.resolvers.*
import com.jetbrains.pluginverifier.jdk.JdkDescriptorCreator
import com.jetbrains.pluginverifier.tests.findMockPluginJarPath
import com.jetbrains.pluginverifier.verifiers.resolution.classDump.InnerClassExampleDump
import com.jetbrains.pluginverifier.verifiers.resolution.classDump.JavaEnumExampleDump
import com.jetbrains.pluginverifier.verifiers.resolution.classDump.KotlinDefaultMethodInterfaceDump
import com.jetbrains.pluginverifier.verifiers.resolution.classDump.KotlinDefaultMethodStubDump
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.*
import org.junit.Test
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.ASM7
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FrameNode
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.LineNumberNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.VarInsnNode
import java.nio.file.Path

class MethodsTest {
  @Test
  fun testSearchParentOverrides() {
    val javaHome = System.getProperty("java.home")
    val jdkDescriptor = JdkDescriptorCreator.createJdkDescriptor(Path.of(javaHome))

    val jdkResolver = jdkDescriptor.jdkResolver
    val mockPluginResolver = createTestResolver()
    val resolver = CompositeResolver.create(
      jdkResolver, mockPluginResolver
    )

    val observedClassName = "mock/plugin/method/MethodOverriddenFromJavaLangObject"
    val overriddenMethodName = "toString"
    val observerClassResult = resolver.resolveClass(observedClassName)
    if (observerClassResult !is ResolutionResult.Found) {
      fail("Class '$observedClassName' not found in the mock plugin by resolver")
    } else {
      val classNode = observerClassResult.value
      val toStringMethod = classNode.methods.first { it.name == overriddenMethodName }
      val classFile = ClassFileAsm(observerClassResult.value, observerClassResult.fileOrigin)
      val method: Method = MethodAsm(classFile, toStringMethod)

      val methodOverrides = method.searchParentOverrides(resolver)
      assertThat(methodOverrides.size, `is`(0))
    }
  }

  @Test
  fun `method parameters match`() {
    val resolver = createJdkResolver()
    val arrayListBinaryName = "java/util/ArrayList"
    val arrayList = resolver.resolveClass(arrayListBinaryName)
    val linkedListBinaryName = "java/util/LinkedList"
    val linkedList = resolver.resolveClass(linkedListBinaryName)
    if (arrayList !is ResolutionResult.Found) {
      fail("Class '$arrayListBinaryName' not found in the mock plugin by resolver")
      return
    }
    if (linkedList !is ResolutionResult.Found) {
      fail("Class '$linkedListBinaryName' not found in the mock plugin by resolver")
      return
    }
    val methodName = "set"
    val arrayListSetAsmMethod = arrayList.findMethodByName(methodName)
    val linkedListSetAsmMethod = linkedList.findMethodByName(methodName)


    if (arrayListSetAsmMethod == null) {
      fail("Method '$methodName' not found in '$arrayListBinaryName'")
      return
    }

    if (linkedListSetAsmMethod == null) {
      fail("Method '$methodName' not found in '$linkedListBinaryName'")
      return
    }
    val arrayListMethod = MethodAsm(arrayList.asClassFile(), arrayListSetAsmMethod)
    val linkedListSetMethod = MethodAsm(linkedList.asClassFile(), linkedListSetAsmMethod)

    sameParameters(arrayListMethod, linkedListSetMethod)
  }

  @Test
  fun `method parameter count does not match`() {
    val arrayListBinaryName = "java/util/ArrayList"

    val resolver = createJdkResolver()
    // E set(int,E)
    val set = resolver.findMethodInClass(arrayListBinaryName, "(ILjava/lang/Object;)Ljava/lang/Object;")
    // int size()
    val size = resolver.findMethodInClass(arrayListBinaryName, "()I")

    assertFalse(sameParameters(set, size))
  }

  @Test
  fun `method parameters do not match`() {
    val arrayListBinaryName = "java/util/ArrayList"

    val resolver = createJdkResolver()
    // E set(int,E)
    val set = resolver.findMethodInClass(arrayListBinaryName, "(ILjava/lang/Object;)Ljava/lang/Object;")
    // List<E> subList(int fromIndex, int toIndex)
    val subList = resolver.findMethodInClass(arrayListBinaryName, "(II)Ljava/util/List;")

    assertFalse(sameParameters(set, subList))
  }

  @Test
  fun `method parameters override with the same parameters`() {
    val abstractCollectionBinaryName = "java/util/AbstractCollection"
    val objectBinaryName = "java/lang/Object"

    val toStringDesc = "()Ljava/lang/String;"

    val resolver = createJdkResolver()
    val arrayListToString = resolver.findMethodInClass(abstractCollectionBinaryName, toStringDesc)
    val objectToString = resolver.findMethodInClass(objectBinaryName, toStringDesc)

    assertTrue(arrayListToString.isOverriding(objectToString, resolver))
  }

  @Test
  fun `method parameters override with the covariancy`() {
    val pkg = "mock/plugin/overrideOnly/covariant"

    val childName: BinaryClassName = "$pkg/Child"
    val parentName: BinaryClassName = "$pkg/Parent"

    val descriptorInChild = "()L$pkg/ChildResult;"
    val descriptorInParent = "()L$pkg/ParentResult;"

    val resolver = createTestResolver()
    val methodInChild = resolver.findMethodInClass(childName, descriptorInChild)
    val methodInParent = resolver.findMethodInClass(parentName, descriptorInParent)

    assertTrue(methodInChild.isOverriding(methodInParent, resolver))
  }

  @Test
  fun `enum method parameter annotations should be attached at the correct index`() {
    val origin = object : FileOrigin {
      override val parent = null
    }

    val classNode = ClassNode(ASM7)
    val classReader = ClassReader(JavaEnumExampleDump.dump())
    classReader.accept(classNode, 0)

    val classFile = ClassFileAsm(classNode, origin)
    val initMethod = classFile.methods.find { it.name == "<init>" }!!

    val methodParameters = initMethod.methodParameters
    // methodParameters should contain enumName: String, ordinal: Int, x: Int, propertyKey: String, propertyKey2: String
    assertEquals(5, methodParameters.size)
    methodParameters[0].apply {
      // Synthetic parameter denoting the name of the enum
      assertTrue(annotations.isEmpty())
    }
    methodParameters[1].apply {
      // Synthetic parameter denoting the ordinal of the enum
      assertTrue(annotations.isEmpty())
    }
    methodParameters[2].apply {
      assertTrue(annotations.isEmpty())
    }
    methodParameters[3].apply {
      assertEquals(1, annotations.size)
      assertEquals("Lorg/jetbrains/annotations/PropertyKey;", annotations[0].desc)
    }
    methodParameters[4].apply {
      assertEquals(2, annotations.size)
      assertEquals("Lorg/jetbrains/annotations/NotNull;", annotations[0].desc)
      assertEquals("Lorg/jetbrains/annotations/PropertyKey;", annotations[1].desc)
    }
  }

  @Test
  fun `inner class method parameter annotations should be attached at the correct index`() {
    val origin = object : FileOrigin {
      override val parent = null
    }

    val classNode = ClassNode(ASM7)
    val classReader = ClassReader(InnerClassExampleDump.nonStaticInnerDump())
    classReader.accept(classNode, 0)

    val classFile = ClassFileAsm(classNode, origin)
    val initMethod = classFile.methods.find { it.name == "<init>" }!!

    val methodParameters = initMethod.methodParameters
    // methodParameters should contain this$0: InnerClassExample, name: String, x: Int, name2: String
    assertEquals(4, methodParameters.size)
    methodParameters[0].apply {
      assertEquals("this$0", name) // Synthetic parameter referencing outer instance
      assertTrue(annotations.isEmpty())
    }
    methodParameters[1].apply {
      assertEquals("name", name)
      assertEquals(1, annotations.size)
      assertEquals("Lorg/jetbrains/annotations/PropertyKey;", annotations[0].desc)
    }
    methodParameters[2].apply {
      assertEquals("x", name)
      assertTrue(annotations.isEmpty())
    }
    methodParameters[3].apply {
      assertEquals("name2", name)
      assertEquals(1, annotations.size)
      assertEquals("Lorg/jetbrains/annotations/PropertyKey;", annotations[0].desc)
    }
  }

  @Test
  fun `static inner class method parameter annotations should be attached at the correct index`() {
    val origin = object : FileOrigin {
      override val parent = null
    }

    val classNode = ClassNode(ASM7)
    val classReader = ClassReader(InnerClassExampleDump.staticInnerDump())
    classReader.accept(classNode, 0)

    val classFile = ClassFileAsm(classNode, origin)
    val initMethod = classFile.methods.find { it.name == "<init>" }!!

    val methodParameters = initMethod.methodParameters
    // methodParameters should contain name: String, x: Int, name2: String
    assertEquals(3, methodParameters.size)
    methodParameters[0].apply {
      assertEquals("name", name)
      assertEquals(1, annotations.size)
      assertEquals("Lorg/jetbrains/annotations/PropertyKey;", annotations[0].desc)
    }
    methodParameters[1].apply {
      assertEquals("x", name)
      assertTrue(annotations.isEmpty())
    }
    methodParameters[2].apply {
      assertEquals("name2", name)
      assertEquals(1, annotations.size)
      assertEquals("Lorg/jetbrains/annotations/PropertyKey;", annotations[0].desc)
    }
  }

  @Test
  fun `Kotlin ENABLE-mode dump fixtures parse and expose the expected methods`() {
    val origin = object : FileOrigin {
      override val parent = null
    }

    val interfaceClassNode = ClassNode(ASM7)
    ClassReader(KotlinDefaultMethodInterfaceDump.dump()).accept(interfaceClassNode, 0)
    val interfaceClassFile = ClassFileAsm(interfaceClassNode, origin)
    assertEquals(setOf("entryPoint", "internalMethod"), interfaceClassFile.methods.map { it.name }.toSet())

    val internalMethod = interfaceClassFile.methods.first { it.name == "internalMethod" }
    assertTrue(internalMethod.annotations.any { it.desc == "Lorg/jetbrains/annotations/ApiStatus\$Internal;" })

    val implClassNode = ClassNode(ASM7)
    ClassReader(KotlinDefaultMethodStubDump.dump()).accept(implClassNode, 0)
    val implClassFile = ClassFileAsm(implClassNode, origin)
    assertEquals(setOf("<init>", "entryPoint", "internalMethod"), implClassFile.methods.map { it.name }.toSet())

    val stubMethod = implClassFile.methods.first { it.name == "internalMethod" }
    val realInstructions = stubMethod.instructions.filterNot {
      it is LabelNode || it is LineNumberNode || it is FrameNode
    }
    assertEquals(3, realInstructions.size)
    assertEquals(Opcodes.ALOAD, (realInstructions[0] as VarInsnNode).opcode)
    val invoke = realInstructions[1] as MethodInsnNode
    assertEquals(Opcodes.INVOKESPECIAL, invoke.opcode)
    assertEquals("com/jetbrains/test/TestInterface", invoke.owner)
    assertEquals("internalMethod", invoke.name)
    assertTrue(invoke.itf)
    assertEquals(Opcodes.RETURN, realInstructions[2].opcode)
  }

  @Test
  fun `real Kotlin 2_2 ENABLE-mode compiler stub overriding an internal default method is detected as a compatibility stub`() {
    val origin = object : FileOrigin {
      override val parent = null
    }

    val interfaceClassNode = ClassNode(ASM7)
    ClassReader(KotlinDefaultMethodInterfaceDump.dump()).accept(interfaceClassNode, 0)
    val internalMethod = ClassFileAsm(interfaceClassNode, origin).methods.first { it.name == "internalMethod" }

    val implClassNode = ClassNode(ASM7)
    ClassReader(KotlinDefaultMethodStubDump.dump()).accept(implClassNode, 0)
    val stubMethod = ClassFileAsm(implClassNode, origin).methods.first { it.name == "internalMethod" }

    assertTrue(stubMethod.isKotlinDefaultMethodCompatibilityStub(internalMethod))
  }

  @Test
  fun `compiler stub overriding a two-parameter default method with a wide-type parameter is detected as a compatibility stub`() {
    val descriptor = "(JLjava/lang/String;)J"
    val (overriddenMethod, overridingMethod) = buildOverriddenAndOverridingMethod(
      overriddenClassName = "mock/plugin/stub/StubInterface",
      overriddenClassAccess = Opcodes.ACC_PUBLIC or Opcodes.ACC_ABSTRACT or Opcodes.ACC_INTERFACE,
      methodName = "combine",
      descriptor = descriptor,
      overriddenMethodInstructions = listOf(InsnNode(Opcodes.LCONST_0), InsnNode(Opcodes.LRETURN)),
      overridingClassName = "mock/plugin/stub/StubImpl",
      overridingMethodInstructions = listOf(
        VarInsnNode(Opcodes.ALOAD, 0),
        VarInsnNode(Opcodes.LLOAD, 1),
        VarInsnNode(Opcodes.ALOAD, 3),
        MethodInsnNode(Opcodes.INVOKESPECIAL, "mock/plugin/stub/StubInterface", "combine", descriptor, true),
        InsnNode(Opcodes.LRETURN)
      )
    )

    assertTrue(overridingMethod.isKotlinDefaultMethodCompatibilityStub(overriddenMethod))
  }

  @Test
  fun `override with additional logic beyond the forwarding call is not a compatibility stub`() {
    val descriptor = "()V"
    val (overriddenMethod, overridingMethod) = buildOverriddenAndOverridingMethod(
      overriddenClassName = "mock/plugin/stub/StubInterface2",
      overriddenClassAccess = Opcodes.ACC_PUBLIC or Opcodes.ACC_ABSTRACT or Opcodes.ACC_INTERFACE,
      methodName = "greet",
      descriptor = descriptor,
      overriddenMethodInstructions = listOf(InsnNode(Opcodes.RETURN)),
      overridingClassName = "mock/plugin/stub/StubImpl2",
      overridingMethodInstructions = listOf(
        VarInsnNode(Opcodes.ALOAD, 0),
        MethodInsnNode(Opcodes.INVOKESPECIAL, "mock/plugin/stub/StubInterface2", "greet", descriptor, true),
        InsnNode(Opcodes.NOP),
        InsnNode(Opcodes.RETURN)
      )
    )

    assertFalse(overridingMethod.isKotlinDefaultMethodCompatibilityStub(overriddenMethod))
  }

  @Test
  fun `override that calls a different method than the overridden one is not a compatibility stub`() {
    val descriptor = "()V"
    val (overriddenMethod, overridingMethod) = buildOverriddenAndOverridingMethod(
      overriddenClassName = "mock/plugin/stub/StubInterface3",
      overriddenClassAccess = Opcodes.ACC_PUBLIC or Opcodes.ACC_ABSTRACT or Opcodes.ACC_INTERFACE,
      methodName = "greet",
      descriptor = descriptor,
      overriddenMethodInstructions = listOf(InsnNode(Opcodes.RETURN)),
      overridingClassName = "mock/plugin/stub/StubImpl3",
      overridingMethodInstructions = listOf(
        VarInsnNode(Opcodes.ALOAD, 0),
        MethodInsnNode(Opcodes.INVOKESPECIAL, "mock/plugin/stub/StubInterface3", "somethingElse", descriptor, true),
        InsnNode(Opcodes.RETURN)
      )
    )

    assertFalse(overridingMethod.isKotlinDefaultMethodCompatibilityStub(overriddenMethod))
  }

  @Test
  fun `override of a non-interface (abstract class) method is never a compatibility stub`() {
    val descriptor = "()V"
    val (overriddenMethod, overridingMethod) = buildOverriddenAndOverridingMethod(
      overriddenClassName = "mock/plugin/stub/AbstractBase",
      overriddenClassAccess = Opcodes.ACC_PUBLIC or Opcodes.ACC_ABSTRACT,
      methodName = "greet",
      descriptor = descriptor,
      overriddenMethodInstructions = listOf(InsnNode(Opcodes.RETURN)),
      overridingClassName = "mock/plugin/stub/BaseImpl",
      overridingMethodInstructions = listOf(
        VarInsnNode(Opcodes.ALOAD, 0),
        MethodInsnNode(Opcodes.INVOKESPECIAL, "mock/plugin/stub/AbstractBase", "greet", descriptor, false),
        InsnNode(Opcodes.RETURN)
      )
    )

    assertFalse(overridingMethod.isKotlinDefaultMethodCompatibilityStub(overriddenMethod))
  }

  @Test
  fun `compiler stub inheriting the default method indirectly forwards to the direct superinterface and is detected as a compatibility stub`() {
    // Kotlin 2.2.0 emits `INVOKESPECIAL B.f()` for `interface A { fun f() {} }; interface B : A; class Impl : B` —
    // when the default method is inherited indirectly, invokespecial targets the direct superinterface, not the
    // originally declaring one.
    val descriptor = "()V"
    val (overriddenMethod, overridingMethod) = buildOverriddenAndOverridingMethod(
      overriddenClassName = "mock/plugin/stub/GrandParentInterface",
      overriddenClassAccess = Opcodes.ACC_PUBLIC or Opcodes.ACC_ABSTRACT or Opcodes.ACC_INTERFACE,
      methodName = "f",
      descriptor = descriptor,
      overriddenMethodInstructions = listOf(InsnNode(Opcodes.RETURN)),
      overridingClassName = "mock/plugin/stub/IndirectImpl",
      overridingClassInterfaces = listOf("mock/plugin/stub/ParentInterface"),
      overridingMethodInstructions = listOf(
        VarInsnNode(Opcodes.ALOAD, 0),
        MethodInsnNode(Opcodes.INVOKESPECIAL, "mock/plugin/stub/ParentInterface", "f", descriptor, true),
        InsnNode(Opcodes.RETURN)
      )
    )

    assertTrue(overridingMethod.isKotlinDefaultMethodCompatibilityStub(overriddenMethod))
  }

  @Test
  fun `forwarding call to an interface that is not a superinterface of the caller is not a compatibility stub`() {
    val descriptor = "()V"
    val (overriddenMethod, overridingMethod) = buildOverriddenAndOverridingMethod(
      overriddenClassName = "mock/plugin/stub/UnrelatedInterface",
      overriddenClassAccess = Opcodes.ACC_PUBLIC or Opcodes.ACC_ABSTRACT or Opcodes.ACC_INTERFACE,
      methodName = "f",
      descriptor = descriptor,
      overriddenMethodInstructions = listOf(InsnNode(Opcodes.RETURN)),
      overridingClassName = "mock/plugin/stub/UnrelatedImpl",
      overridingClassInterfaces = listOf("mock/plugin/stub/SomeOtherInterface"),
      overridingMethodInstructions = listOf(
        VarInsnNode(Opcodes.ALOAD, 0),
        MethodInsnNode(Opcodes.INVOKESPECIAL, "mock/plugin/stub/SomeThirdInterface", "f", descriptor, true),
        InsnNode(Opcodes.RETURN)
      )
    )

    assertFalse(overridingMethod.isKotlinDefaultMethodCompatibilityStub(overriddenMethod))
  }

  @Test
  fun `a stub-shaped method whose own name differs from the invoked method is not a compatibility stub`() {
    val descriptor = "()V"
    val (overriddenMethod, overridingMethod) = buildOverriddenAndOverridingMethod(
      overriddenClassName = "mock/plugin/stub/StubInterface4",
      overriddenClassAccess = Opcodes.ACC_PUBLIC or Opcodes.ACC_ABSTRACT or Opcodes.ACC_INTERFACE,
      methodName = "internalMethod",
      descriptor = descriptor,
      overriddenMethodInstructions = listOf(InsnNode(Opcodes.RETURN)),
      overridingClassName = "mock/plugin/stub/StubImpl4",
      // `void refresh() { StubInterface4.super.internalMethod(); }` — a deliberate internal-API use
      overridingMethodName = "refresh",
      overridingMethodInstructions = listOf(
        VarInsnNode(Opcodes.ALOAD, 0),
        MethodInsnNode(Opcodes.INVOKESPECIAL, "mock/plugin/stub/StubInterface4", "internalMethod", descriptor, true),
        InsnNode(Opcodes.RETURN)
      )
    )

    assertFalse(overridingMethod.isKotlinDefaultMethodCompatibilityStub(overriddenMethod))
  }

  @Test
  fun `a stub-shaped method whose own descriptor differs from the invoked method is not a compatibility stub`() {
    val (overriddenMethod, overridingMethod) = buildOverriddenAndOverridingMethod(
      overriddenClassName = "mock/plugin/stub/StubInterface5",
      overriddenClassAccess = Opcodes.ACC_PUBLIC or Opcodes.ACC_ABSTRACT or Opcodes.ACC_INTERFACE,
      methodName = "internalMethod",
      descriptor = "()V",
      overriddenMethodInstructions = listOf(InsnNode(Opcodes.RETURN)),
      overridingClassName = "mock/plugin/stub/StubImpl5",
      // `void internalMethod(int unused) { StubInterface5.super.internalMethod(); }` — an overload, not an override
      overridingDescriptor = "(I)V",
      overridingMethodInstructions = listOf(
        VarInsnNode(Opcodes.ALOAD, 0),
        MethodInsnNode(Opcodes.INVOKESPECIAL, "mock/plugin/stub/StubInterface5", "internalMethod", "()V", true),
        InsnNode(Opcodes.RETURN)
      )
    )

    assertFalse(overridingMethod.isKotlinDefaultMethodCompatibilityStub(overriddenMethod))
  }

  private fun buildOverriddenAndOverridingMethod(
    overriddenClassName: String,
    overriddenClassAccess: Int,
    methodName: String,
    descriptor: String,
    overriddenMethodInstructions: List<AbstractInsnNode>,
    overridingClassName: String,
    overridingMethodInstructions: List<AbstractInsnNode>,
    overridingClassInterfaces: List<String> = listOf(overriddenClassName),
    overridingMethodName: String = methodName,
    overridingDescriptor: String = descriptor
  ): Pair<Method, Method> {
    val origin = object : FileOrigin {
      override val parent = null
    }

    val overriddenClassNode = ClassNode(Opcodes.ASM9).apply {
      version = Opcodes.V1_8
      access = overriddenClassAccess
      name = overriddenClassName
      superName = "java/lang/Object"
      methods.add(MethodNode().apply {
        access = Opcodes.ACC_PUBLIC
        name = methodName
        desc = descriptor
        overriddenMethodInstructions.forEach { instructions.add(it) }
      })
    }
    val overriddenMethod = ClassFileAsm(overriddenClassNode, origin).methods.first { it.name == methodName }

    val overridingClassNode = ClassNode(Opcodes.ASM9).apply {
      version = Opcodes.V1_8
      access = Opcodes.ACC_PUBLIC
      name = overridingClassName
      superName = "java/lang/Object"
      interfaces = overridingClassInterfaces.toMutableList()
      methods.add(MethodNode().apply {
        access = Opcodes.ACC_PUBLIC
        name = overridingMethodName
        desc = overridingDescriptor
        overridingMethodInstructions.forEach { instructions.add(it) }
      })
    }
    val overridingMethod = ClassFileAsm(overridingClassNode, origin).methods.first { it.name == overridingMethodName }

    return overriddenMethod to overridingMethod
  }

  @Throws(AssertionError::class)
  private fun Resolver.findMethodInClass(cls: BinaryClassName, descriptor: String): MethodAsm {
    val resolutionResult = resolveClass(cls)
    if (resolutionResult !is ResolutionResult.Found) {
      throw AssertionError("Class '$cls' not found in the mock plugin by resolver")
    }
    val methodNode = resolutionResult.findMethodByDesc(descriptor)
      ?: throw AssertionError("Method '$descriptor' not found in '$cls'")

    return MethodAsm(resolutionResult.asClassFile(), methodNode)
  }

  private fun ResolutionResult.Found<ClassNode>.findMethodByName(methodName: String): MethodNode? {
    return value.methods.find {
      it.name == methodName
    }
  }

  private fun ResolutionResult.Found<ClassNode>.findMethodByDesc(methodDesc: String): MethodNode? {
    return value.methods.find {
      it.desc == methodDesc
    }
  }

  private fun ResolutionResult.Found<ClassNode>.asClassFile(): ClassFile {
    return ClassFileAsm(value, fileOrigin)
  }

  private fun createTestResolver(): Resolver =
    LazyJarResolver(
      findMockPluginJarPath(),
      Resolver.ReadMode.FULL,
      object : FileOrigin {
        override val parent: FileOrigin? = null
      }
    )

  private fun createJdkResolver(): Resolver {
    val javaHome = System.getProperty("java.home")
    val jdkDescriptor = JdkDescriptorCreator.createJdkDescriptor(Path.of(javaHome))

    return jdkDescriptor.jdkResolver
  }

  /**
   * Generates a class file similar to a Java enum.
   * @param additionalInitParams Additional parameters for the enum's constructor besides the 2 synthetic ones.
   *  The String is the type of the parameter in JVM Descriptor form, while the lambda can be used to add
   *  additional initializing logic to the <clinit> function (e.g. loading a constant as an argument for our parameter)
   * @param addAnnotation Whether to add a @PropertyKey annotation to the list of RuntimeInvisibleParameterAnnotations.
   *  Currently, it will simply decide whether to add one at index 0.
   */
  private fun generateJavaInnerClass(): ClassNode {
    val resourceBundleName = "messages.Messages"

    val outerClassName = "com/jetbrains/InnerClassExample"
    val innerClassName = "${outerClassName}\$InnerClass"



    val innerClass = ClassNode(Opcodes.ASM9).apply {
      version = Opcodes.V1_8 // Java 8
      access = Opcodes.ACC_PUBLIC or Opcodes.ACC_SUPER
      name = innerClassName
      outerClass = outerClassName

      val descriptor = "(L${outerClassName};ILjava/lang/String;)V"
      val initMethodNode = MethodNode().apply {
        access = Opcodes.ACC_PUBLIC
        name = "<init>"
        desc = descriptor

        invisibleParameterAnnotations = arrayOf(
          null,
          listOf(
            AnnotationNode("Lorg/jetbrains/annotations/PropertyKey;").apply { values = listOf("resourceBundle", resourceBundleName) }
          ),
          null)

        // return;
        instructions.add(InsnNode(Opcodes.RETURN))
      }

      methods.add(initMethodNode)
    }

    /*val outerClass = ClassNode(Opcodes.ASM9).apply {
      version = Opcodes.V1_8 // Java 8
      access = Opcodes.ACC_PUBLIC or Opcodes.ACC_SUPER
      name = outerClassName

      val callSiteMethodNode = MethodNode().apply {
        name = "callSite"
        desc = "()V"

//        tryCatchBlocks = listOf()

        // Dummy callSite function
        // It creates the enum constant `ENUM_NAME` with ordinal 0. Its first non-synthetic parameter is a property key.
        instructions.add(TypeInsnNode(Opcodes.NEW, outerClassName))
        instructions.add(VarInsnNode(25, 0)) // Loads `this`
        instructions.add(LdcInsnNode(0))
        instructions.add(LdcInsnNode("non.existing.key"))
        instructions.add(MethodInsnNode(Opcodes.INVOKESPECIAL, outerClassName, "<init>", "(L${outerClassName};ILjava/lang/String;)V"))
        instructions.add(InsnNode(Opcodes.RETURN))

        maxStack = 4
      }

      methods.add(callSiteMethodNode)
    }*/

    return innerClass
  }
}