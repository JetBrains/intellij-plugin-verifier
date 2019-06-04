package org.jetbrains.ide.diff.builder.signatures

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.ide.IdeManager
import com.jetbrains.plugin.structure.ide.classes.IdeResolverCreator
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileAsm
import com.jetbrains.pluginverifier.verifiers.resolution.Field
import org.jetbrains.ide.diff.builder.BaseOldNewIdesTest
import org.jetbrains.ide.diff.builder.api.FakeClassFileOrigin
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.BeforeClass
import org.junit.Test

/**
 * Asserts that string presentations for API elements found
 * in byte-code are evaluated according to format expected
 * by the external annotations system,
 * which is `com.intellij.psi.util.PsiFormatUtil.getExternalName`.
 */
class ExternalNameFormatTest : BaseOldNewIdesTest() {

  companion object {

    lateinit var ideResolver: Resolver

    @BeforeClass
    @JvmStatic
    fun createIdeResolver() {
      val oldIdeFile = getOldIdeFile()
      val ide = IdeManager.createManager().createIde(oldIdeFile)
      ideResolver = IdeResolverCreator.createIdeResolver(ide)
    }

    @AfterClass
    @JvmStatic
    fun closeIdeResolver() {
      ideResolver.close()
    }
  }

  private val className2externalName = mapOf(
      "same/Same" to "same.Same",
      "same/Same\$Inner" to "same.Same.Inner",
      "same/Same\$Nested" to "same.Same.Nested"
  )

  private val fieldName2externalName = mapOf(
      "f1" to "same.Same f1",
      "f2" to "same.Same f2",
      "f3" to "same.Same f3",
      "f4" to "same.Same f4"
  )

  @Test
  fun `check class names`() {
    val classNodes = ideResolver.allClasses.map { findClassFile(it) }
    checkClasses(classNodes)
  }

  @Test
  fun `check many signatures from A`() {
    val aClass = findClassFile("same/Same")
    val methodName2externalName = mapOf(
        "<init>" to "same.Same Same(int)",
        "m1" to "same.Same void m1()",
        "m2" to "same.Same int m2()",
        "m3" to "same.Same java.lang.String m3()",
        "m4" to "same.Same void m4(java.lang.String)",
        "m5" to "same.Same void m5(java.util.List<java.lang.String>)",
        "m6" to "same.Same T m6()",
        "m7" to "same.Same void m7(T)",
        "m8" to "same.Same T m8(S)",
        "m9" to "same.Same void m9(java.util.Map<java.lang.String,java.lang.Integer>)",
        "m10" to "same.Same void m10(java.util.Map<K,V>)",
        "m11" to "same.Same int[] m11()",
        "m12" to "same.Same java.lang.String[][] m12()",
        "m13" to "same.Same java.util.List<java.lang.Comparable<? extends java.lang.Number>> m13()",
        "m14" to "same.Same java.lang.Class<? extends E> m14()",
        "m15" to "same.Same java.lang.Class<? super E> m15()",
        "m16" to "same.Same java.util.Map<java.lang.Object,java.lang.String> m16(java.lang.Object, java.util.Map<java.lang.Object,java.lang.String>, T)",
        "m17" to "same.Same void m17(java.lang.Class<?>, java.lang.Class<?>[][])"
    )
    checkMethods(aClass, methodName2externalName)
  }

  @Test
  fun `check field names`() {
    val aClass = findClassFile("same/Same")
    checkFields(aClass.fields)
  }

  private fun findClassFile(className: String): ClassFile =
      ClassFileAsm(ideResolver.findClass(className)!!, FakeClassFileOrigin)

  /**
   * Checks that external name of empty constructor of an inner class
   * is evaluated without implicit parameter of enclosing class.
   *
   * ```
   * public class A {
   *   public class B {
   *     public B() { }
   *   }
   * }
   *
   * descriptor = (LA;)V
   * but external name must be A.B B()
   */
  @Test
  fun `check default constructor of an inner class name`() {
    val innerClass = findClassFile("same/Same\$Inner")
    val nestedClass = findClassFile("same/Same\$Nested")
    checkMethods(innerClass, mapOf("<init>" to "same.Same.Inner Inner()"))
    checkMethods(nestedClass, mapOf("<init>" to "same.Same.Nested Nested()"))
  }

  private fun checkClasses(classNodes: List<ClassFile>) {
    for ((className, expectedName) in className2externalName) {
      val classFile = classNodes.find { it.name == className }
      assertNotNull(className, classFile)
      assertNotNull(classFile!!.name, expectedName)

      val actualName = classFile.location.toSignature().externalPresentation
      assertEquals(expectedName, actualName)
    }
  }

  private fun checkMethods(aClass: ClassFile, methodName2externalName: Map<String, String>) {
    for (methodNode in aClass.methods) {
      val expectedName = methodName2externalName[methodNode.name]
      assertNotNull(methodNode.name, expectedName)

      val actualName = methodNode.location.toSignature().externalPresentation
      assertEquals(expectedName, actualName)
    }
  }

  private fun checkFields(fieldNodes: Sequence<Field>) {
    for (fieldNode in fieldNodes) {
      val expectedName = fieldName2externalName[fieldNode.name]
      assertNotNull(fieldNode.name, expectedName)

      val actualName = fieldNode.location.toSignature().externalPresentation
      assertEquals(expectedName, actualName)
    }
  }
}