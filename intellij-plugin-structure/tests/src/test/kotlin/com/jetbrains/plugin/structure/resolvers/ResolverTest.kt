package com.jetbrains.plugin.structure.resolvers

import com.jetbrains.plugin.structure.classes.resolvers.*
import org.junit.Assert.*
import org.junit.Test
import org.objectweb.asm.tree.ClassNode

class ResolverTest {
  @Test
  fun `empty cache doesnt contain classes`() {
    val cacheResolver = CacheResolver(EmptyResolver)
    assertEquals(ResolutionResult.NotFound, cacheResolver.resolveClass("a"))
    assertTrue(cacheResolver.allClasses.isEmpty())
    assertEquals(emptySet<String>(), cacheResolver.allPackages)
  }

  @Test
  fun `cache with one class`() {
    val className = "a"
    val classNode = ClassNode()
    classNode.name = className
    val classFileOrigin = object : ClassFileOrigin {
      override val parent: ClassFileOrigin? = null
    }
    val cacheResolver = CacheResolver(
        FixedClassesResolver.create(listOf(classNode), classFileOrigin, Resolver.ReadMode.FULL)
    )
    assertEquals(1, cacheResolver.allClasses.size)
    val found = cacheResolver.resolveClass(className) as ResolutionResult.Found
    assertEquals(classNode, found.classNode)
    assertEquals(classFileOrigin, found.classFileOrigin)
    assertEquals(setOf(""), cacheResolver.allPackages)
    assertTrue(cacheResolver.containsPackage(""))
  }

  @Test
  fun `composite resolver search order is equal to class-path`() {
    val commonPackage = "some/package"

    val sameClass = "$commonPackage/Same"
    val sameClassNode1 = ClassNode().apply { name = sameClass }
    val sameClassNode2 = ClassNode().apply { name = sameClass }

    val class1 = "$commonPackage/Some1"
    val class1Node = ClassNode().apply { name = class1 }

    val class2 = "$commonPackage/Some2"
    val class2Node = ClassNode().apply { name = class2 }

    val origin1 = object : ClassFileOrigin {
      override val parent: ClassFileOrigin? = null
    }
    val origin2 = object : ClassFileOrigin {
      override val parent: ClassFileOrigin? = null
    }

    val resolver1 = FixedClassesResolver.create(listOf(class1Node, sameClassNode1), classFileOrigin = origin1)
    val resolver2 = FixedClassesResolver.create(listOf(class2Node, sameClassNode2), classFileOrigin = origin2)

    val resolver = CompositeResolver.create(resolver1, resolver2)

    assertEquals(setOf("some", "some/package"), resolver.allPackages)
    assertEquals(setOf(sameClass, class1, class2), resolver.allClasses)

    assertSame(origin1, (resolver.resolveClass(class1) as ResolutionResult.Found).classFileOrigin)
    assertSame(origin2, (resolver.resolveClass(class2) as ResolutionResult.Found).classFileOrigin)
    assertSame(origin1, (resolver.resolveClass(sameClass) as ResolutionResult.Found).classFileOrigin)
  }
}