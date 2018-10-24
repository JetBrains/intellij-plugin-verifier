package org.jetbrains.ide.diff.builder.signatures

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.ide.IdeManager
import com.jetbrains.plugin.structure.ide.classes.IdeResolverCreator
import com.jetbrains.pluginverifier.verifiers.*
import org.jetbrains.ide.diff.builder.BaseOldNewIdesTest
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.BeforeClass
import org.junit.Test
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

/**
 * Asserts that string presentations for API elements found
 * in bytecode are evaluated according to format expected
 * by the external annotations system,
 * which is [com.intellij.psi.util.PsiFormatUtil.getExternalName].
 *
 * @see [formatAsExternalName]
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
      "pkg/A" to "pkg.A",
      "pkg/B" to "pkg.B",
      "pkg/C" to "pkg.C",
      "pkg/C\$D" to "pkg.C.D",
      "pkg/E" to "pkg.E",
      "pkg/E\$F" to "pkg.E.F",
      "unchanged/A" to "unchanged.A",
      "removed/A" to "removed.A"
  )

  private val methodName2externalName = mapOf(
      "<init>" to "pkg.A A()",
      "m1" to "pkg.A void m1()",
      "m2" to "pkg.A int m2()",
      "m3" to "pkg.A java.lang.String m3()",
      "m4" to "pkg.A void m4(java.lang.String)",
      "m5" to "pkg.A void m5(java.util.List<java.lang.String>)",
      "m6" to "pkg.A T m6()",
      "m7" to "pkg.A void m7(T)",
      "m8" to "pkg.A T m8(S)",
      "m9" to "pkg.A void m9(java.util.Map<java.lang.String,java.lang.Integer>)",
      "m10" to "pkg.A void m10(java.util.Map<K,V>)",
      "m11" to "pkg.A int[] m11()",
      "m12" to "pkg.A java.lang.String[][] m12()",
      "m13" to "pkg.A java.util.List<java.lang.Comparable<? extends java.lang.Number>> m13()",
      "m14" to "pkg.A java.lang.Class<? extends E> m14()",
      "m15" to "pkg.A java.lang.Class<? super E> m15()",
      "m16" to "pkg.A java.util.Map<java.lang.Object,java.lang.String> m16(java.lang.Object, java.util.Map<java.lang.Object,java.lang.String>, T)",
      "m17" to "pkg.A void m17(java.lang.Class<?>, java.lang.Class<?>[][])"
  )

  private val fieldName2externalName = mapOf(
      "f1" to "pkg.A f1",
      "f2" to "pkg.A f2",
      "f3" to "pkg.A f3",
      "f4" to "pkg.A f4"
  )

  @Test
  fun `check class names`() {
    val classNodes = ideResolver.allClasses.map { ideResolver.findClass(it)!! }
    checkClasses(classNodes)
  }

  @Test
  fun `check method names`() {
    val aClass = ideResolver.findClass("pkg/A")!!
    val methodNodes = aClass.getMethods().orEmpty()
    checkMethods(methodNodes, aClass)
  }

  @Test
  fun `check field names`() {
    val aClass = ideResolver.findClass("pkg/A")!!
    val fieldNodes = aClass.getFields().orEmpty()
    checkFields(fieldNodes, aClass)
  }

  @Test
  fun `check default constructor of an inner class name`() {
    val innerClass = ideResolver.findClass("pkg/E\$F")!!
    checkEmptyConstructorOfInnerClass(innerClass)
  }

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
  private fun checkEmptyConstructorOfInnerClass(innerClass: ClassNode) {
    val emptyInnerCtr = innerClass.getMethods()!!.find { it.name == "<init>" }!!
    assertEquals("(Lpkg/E;)V", emptyInnerCtr.desc)

    val externalName = createMethodLocation(innerClass, emptyInnerCtr).toSignature().externalPresentation
    assertEquals("pkg.E.F F()", externalName)
  }

  private fun checkClasses(classNodes: List<ClassNode>) {
    for (classNode in classNodes) {
      val expectedName = className2externalName[classNode.name]
      assertNotNull(classNode.name, expectedName)

      val actualName = classNode.createClassLocation().toSignature().externalPresentation
      assertEquals(expectedName, actualName)
    }
  }

  private fun checkMethods(methodNodes: List<MethodNode>, aClass: ClassNode) {
    for (methodNode in methodNodes) {
      val expectedName = methodName2externalName[methodNode.name]
      assertNotNull(methodNode.name, expectedName)

      val actualName = createMethodLocation(aClass, methodNode).toSignature().externalPresentation
      assertEquals(expectedName, actualName)
    }
  }

  private fun checkFields(fieldNodes: List<FieldNode>, aClass: ClassNode) {
    for (fieldNode in fieldNodes) {
      val expectedName = fieldName2externalName[fieldNode.name]
      assertNotNull(fieldNode.name, expectedName)

      val actualName = createFieldLocation(aClass, fieldNode).toSignature().externalPresentation
      assertEquals(expectedName, actualName)
    }
  }
}