package com.jetbrains.plugin.structure.classes.utils

import com.jetbrains.plugin.structure.classes.utils.AsmUtil.readClassNode
import org.junit.Assert
import org.junit.Test

class KtClassResolverTest {
  @Test
  fun `internal class is resolved as internal`() {
    val classResolver = KtClassResolver()

    val mockInternalClass = KtClassResolverTest::class.java.getResourceAsStream("MockInternalClass.class").use {
      val classNode = readClassNode("com.jetbrains.plugin.structure.classes.utils.MockInternalClass", it)
      classNode
    }

    val ktClassNode = classResolver[mockInternalClass]
    Assert.assertNotNull(ktClassNode)
    ktClassNode!!
    Assert.assertEquals("com/jetbrains/plugin/structure/classes/utils/MockInternalClass", ktClassNode.name)
    Assert.assertEquals(true, ktClassNode.isInternal)
  }
}