package com.jetbrains.plugin.structure.resolvers

import com.jetbrains.plugin.structure.classes.resolvers.CacheResolver
import com.jetbrains.plugin.structure.classes.resolvers.CompositeResolver
import com.jetbrains.plugin.structure.classes.resolvers.EMPTY_RESOLVER
import com.jetbrains.plugin.structure.classes.resolvers.FileOrigin
import com.jetbrains.plugin.structure.classes.resolvers.FixedClassesResolver
import com.jetbrains.plugin.structure.classes.resolvers.ResolutionResult
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import org.junit.Assert.*
import org.junit.Test
import org.objectweb.asm.tree.ClassNode
import java.util.*

class ResolverTest {
  @Test
  fun `empty cache doesnt contain classes`() {
    val cacheResolver = CacheResolver(EMPTY_RESOLVER)
    assertEquals(ResolutionResult.NotFound, cacheResolver.resolveClass("a"))
    assertTrue(cacheResolver.allClasses.isEmpty())
    assertEquals(emptySet<String>(), cacheResolver.allPackages)

    assertEquals(ResolutionResult.NotFound, cacheResolver.resolveExactPropertyResourceBundle("a", Locale.ROOT))
    assertTrue(cacheResolver.allBundleNameSet.isEmpty)
  }

  @Test
  fun `cache with one class`() {
    val className = "a"
    val classNode = ClassNode()
    classNode.name = className
    val fileOrigin = object : FileOrigin {
      override val parent: FileOrigin? = null
    }
    val cacheResolver = CacheResolver(
      FixedClassesResolver.create(listOf(classNode), fileOrigin, readMode = Resolver.ReadMode.FULL)
    )
    assertEquals(1, cacheResolver.allClasses.size)
    val found = cacheResolver.resolveClass(className) as ResolutionResult.Found
    assertEquals(classNode, found.value)
    assertEquals(fileOrigin, found.fileOrigin)
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

    val origin1 = object : FileOrigin {
      override val parent: FileOrigin? = null
    }
    val origin2 = object : FileOrigin {
      override val parent: FileOrigin? = null
    }

    val resolver1 = FixedClassesResolver.create(listOf(class1Node, sameClassNode1), origin1)
    val resolver2 = FixedClassesResolver.create(listOf(class2Node, sameClassNode2), origin2)

    val resolver = CompositeResolver.create(resolver1, resolver2)

    assertEquals(setOf("some", "some/package"), resolver.allPackages)
    assertEquals(setOf(sameClass, class1, class2), resolver.allClasses)

    assertSame(origin1, (resolver.resolveClass(class1) as ResolutionResult.Found).fileOrigin)
    assertSame(origin2, (resolver.resolveClass(class2) as ResolutionResult.Found).fileOrigin)
    assertSame(origin1, (resolver.resolveClass(sameClass) as ResolutionResult.Found).fileOrigin)
  }

  @Test
  fun `composite resolver bundle resolution`() {
    val origin1 = object : FileOrigin {
      override val parent: FileOrigin? = null
    }

    val origin2 = object : FileOrigin {
      override val parent: FileOrigin? = null
    }

    val resolver1 = FixedClassesResolver.create(
      emptyList(),
      origin1,
      propertyResourceBundles = mapOf(
        "messages.SomeBundle" to buildPropertyResourceBundle(
          mapOf(
            "key1" to "value1"
          )
        )
      )
    )

    val resolver2 = FixedClassesResolver.create(
      emptyList(),
      origin2,
      propertyResourceBundles = mapOf(
        "messages.SomeBundle_en" to buildPropertyResourceBundle(
          mapOf(
            "key1" to "value2",
            "en.only.key" to "value3"
          )
        )
      )
    )

    val resolver = CompositeResolver.create(resolver1, resolver2)
    val bundleNameSet = resolver.allBundleNameSet
    assertEquals(setOf("messages.SomeBundle"), bundleNameSet.baseBundleNames)
    assertEquals(setOf("messages.SomeBundle", "messages.SomeBundle_en"), bundleNameSet["messages.SomeBundle"])

    val rootResolveResult = resolver.resolveExactPropertyResourceBundle("messages.SomeBundle", Locale.ROOT) as ResolutionResult.Found
    assertEquals(origin1, rootResolveResult.fileOrigin)
    assertEquals("value1", rootResolveResult.value.getString("key1"))

    val enResolveResult = resolver.resolveExactPropertyResourceBundle("messages.SomeBundle", Locale.ENGLISH) as ResolutionResult.Found
    assertEquals(origin2, enResolveResult.fileOrigin)
    assertEquals("value2", enResolveResult.value.getString("key1"))
    assertEquals("value3", enResolveResult.value.getString("en.only.key"))
  }

  private fun buildPropertyResourceBundle(properties: Map<String, String>): PropertyResourceBundle {
    val reader = properties.entries.joinToString(separator = "\n") {
      "${it.key}=${it.value}"
    }.reader()

    return PropertyResourceBundle(reader)
  }

}