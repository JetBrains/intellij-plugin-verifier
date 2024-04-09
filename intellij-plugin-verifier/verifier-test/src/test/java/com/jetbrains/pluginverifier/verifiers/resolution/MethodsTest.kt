package com.jetbrains.pluginverifier.verifiers.resolution

import com.jetbrains.plugin.structure.classes.resolvers.*
import com.jetbrains.pluginverifier.jdk.JdkDescriptorCreator
import com.jetbrains.pluginverifier.tests.findMockPluginJarPath
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.*
import org.junit.Test
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
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

    val getIndexerInChild = "()L$pkg/ChildResult;"
    val getIndexerInParent = "()L$pkg/ParentResult;"

    val resolver = createTestResolver()
    val arrayListToString = resolver.findMethodInClass(childName, getIndexerInChild)
    val objectToString = resolver.findMethodInClass(parentName, getIndexerInParent)

    assertTrue(arrayListToString.isOverriding(objectToString, resolver))
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
    JarFileResolver(
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
}