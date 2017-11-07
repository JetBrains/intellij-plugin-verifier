package com.jetbrains.plugin.structure.resolvers

import com.jetbrains.plugin.structure.classes.resolvers.CacheResolver
import com.jetbrains.plugin.structure.classes.resolvers.EmptyResolver
import com.jetbrains.plugin.structure.classes.resolvers.FixedClassesResolver
import org.junit.Assert.*
import org.junit.Test
import org.objectweb.asm.tree.ClassNode

class CacheResolverTest {
  @Test
  fun `empty cache doesnt contain classes`() {
    val cacheResolver = CacheResolver(EmptyResolver)
    assertNull(cacheResolver.findClass("a"))
    assertTrue(cacheResolver.allClasses.isEmpty())
  }

  @Test
  fun `cache with one class`() {
    val classNode = ClassNode()
    val className = "a"
    val cacheResolver = CacheResolver(
        FixedClassesResolver(mapOf(className to classNode))
    )
    assertEquals(1, cacheResolver.allClasses.size)
    assertEquals(classNode, cacheResolver.findClass(className))
  }
}