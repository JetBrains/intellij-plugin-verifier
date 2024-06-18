package com.jetbrains.plugin.structure.classes.utils

import com.jetbrains.plugin.structure.classes.utils.AsmUtil.readClassNode
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class KtClassResolverTest {
  private lateinit var ktClassNode: KtClassNode

  @Before
  fun setUp() {
    val classResolver = KtClassResolver()

    val mockInternalClass = KtClassResolverTest::class.java.getResourceAsStream("MockInternalClass.class").use {
      val classNode = readClassNode("com.jetbrains.plugin.structure.classes.utils.MockInternalClass", it)
      classNode
    }

    val ktClassNode = classResolver[mockInternalClass]
    assertNotNull(ktClassNode)
    ktClassNode!!
    assertEquals("com/jetbrains/plugin/structure/classes/utils/MockInternalClass", ktClassNode.name)
    this.ktClassNode = ktClassNode
  }

  @Test
  fun `internal class is resolved as internal`() {
    assertEquals(true, ktClassNode.isInternal)
  }

  @Test
  fun `internal class fields are resolved as internal`() {
    assertTrue(ktClassNode.isInternalField("internalField"))
    assertFalse(ktClassNode.isInternalField("privateField"))
  }
}