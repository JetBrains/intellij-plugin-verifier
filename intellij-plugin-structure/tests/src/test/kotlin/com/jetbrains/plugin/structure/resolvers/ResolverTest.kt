package com.jetbrains.plugin.structure.resolvers

import com.jetbrains.plugin.structure.base.BinaryClassName
import com.jetbrains.plugin.structure.classes.resolvers.CacheResolver
import com.jetbrains.plugin.structure.classes.resolvers.CompositeResolver
import com.jetbrains.plugin.structure.classes.resolvers.DelegatingNamedResolver
import com.jetbrains.plugin.structure.classes.resolvers.EMPTY_RESOLVER
import com.jetbrains.plugin.structure.classes.resolvers.FileOrigin
import com.jetbrains.plugin.structure.classes.resolvers.FixedClassesResolver
import com.jetbrains.plugin.structure.classes.resolvers.ResolutionResult
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import org.junit.Assert.*
import org.junit.Test
import org.objectweb.asm.tree.ClassNode
import java.io.IOException
import java.util.*
import java.util.concurrent.CompletionException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class ResolverTest {
  @Test
  fun `empty cache doesnt contain classes`() {
    val cacheResolver = CacheResolver(EMPTY_RESOLVER)
    assertEquals(ResolutionResult.NotFound, cacheResolver.resolveClass("a"))
    assertTrue(cacheResolver.allClassNames.isEmpty())
    assertEquals(emptySet<String>(), cacheResolver.packages)

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
    assertEquals(1, cacheResolver.allClassNames.size)
    val found = cacheResolver.resolveClass(className) as ResolutionResult.Found
    assertEquals(classNode, found.value)
    assertEquals(fileOrigin, found.fileOrigin)
    assertEquals(setOf(""), cacheResolver.packages)
    assertTrue(cacheResolver.containsPackage(""))
  }

  @Test
  fun `class hits and misses are cached independently`() {
    val calls = mutableMapOf<String, Int>()
    val node = ClassNode().apply { name = "Found" }
    val origin = object : FileOrigin { override val parent: FileOrigin? = null }
    val found = ResolutionResult.Found(node, origin)
    val delegate = object : DelegatingNamedResolver("counting", { EMPTY_RESOLVER }) {
      override fun resolveClass(className: BinaryClassName): ResolutionResult<ClassNode> {
        val key = className.toString()
        calls[key] = (calls[key] ?: 0) + 1
        return if (key == "Found") found else ResolutionResult.NotFound
      }
    }
    val resolver = CacheResolver(delegate, cacheSize = 1)

    assertSame(found, resolver.resolveClass("Found"))
    assertSame(found, resolver.resolveClass("Found"))
    assertSame(ResolutionResult.NotFound, resolver.resolveClass("Missing"))
    assertSame(ResolutionResult.NotFound, resolver.resolveClass("Missing"))
    assertEquals(1, calls["Missing"])
    repeat(64) { index ->
      assertSame(ResolutionResult.NotFound, resolver.resolveClass("Missing$index"))
    }
    assertSame(found, resolver.resolveClass("Found"))
    assertEquals(1, calls["Found"])
  }

  @Test
  fun `class hits do not evict cached misses`() {
    val missingCalls = AtomicInteger()
    val origin = object : FileOrigin { override val parent: FileOrigin? = null }
    val delegate = object : DelegatingNamedResolver("counting", { EMPTY_RESOLVER }) {
      override fun resolveClass(className: BinaryClassName): ResolutionResult<ClassNode> {
        if (className.toString() == "Missing") {
          missingCalls.incrementAndGet()
          return ResolutionResult.NotFound
        }
        return ResolutionResult.Found(ClassNode().apply { name = className.toString() }, origin)
      }
    }
    val resolver = CacheResolver(delegate, cacheSize = 1)

    assertSame(ResolutionResult.NotFound, resolver.resolveClass("Missing"))
    repeat(64) { assertTrue(resolver.resolveClass("Found$it") is ResolutionResult.Found) }
    assertSame(ResolutionResult.NotFound, resolver.resolveClass("Missing"))
    assertEquals(1, missingCalls.get())
  }

  @Test
  fun `concurrent class lookups share successful and missing resolutions`() {
    val origin = object : FileOrigin { override val parent: FileOrigin? = null }
    val results = listOf(ResolutionResult.NotFound, ResolutionResult.Found(ClassNode(), origin))
    for (result in results) {
      val calls = AtomicInteger()
      val start = CountDownLatch(1)
      val delegate = object : DelegatingNamedResolver("counting", { EMPTY_RESOLVER }) {
        override fun resolveClass(className: BinaryClassName): ResolutionResult<ClassNode> {
          calls.incrementAndGet()
          return result
        }
      }
      val resolver = CacheResolver(delegate)
      val executor = Executors.newFixedThreadPool(8)
      try {
        val lookups = (1..8).map {
          executor.submit<ResolutionResult<ClassNode>> {
            check(start.await(5, TimeUnit.SECONDS))
            resolver.resolveClass("Class")
          }
        }
        start.countDown()
        lookups.forEach { assertSame(result, it.get(5, TimeUnit.SECONDS)) }
        assertEquals(1, calls.get())
      } finally {
        executor.shutdownNow()
      }
    }
  }

  @Test
  fun `class resolution exceptions are propagated and not cached`() {
    val calls = AtomicInteger()
    val failure = IllegalStateException("cannot read class")
    val delegate = object : DelegatingNamedResolver("failing", { EMPTY_RESOLVER }) {
      override fun resolveClass(className: BinaryClassName): ResolutionResult<ClassNode> {
        calls.incrementAndGet()
        throw failure
      }
    }
    val resolver = CacheResolver(delegate)

    repeat(2) {
      try {
        resolver.resolveClass("Class")
        fail("Expected class resolution to fail")
      } catch (e: IllegalStateException) {
        assertSame(failure, e)
      }
    }
    assertEquals(2, calls.get())
  }

  @Test
  fun `checked class resolution exceptions retain loading cache wrapping`() {
    val failure = IOException("cannot read class")
    val delegate = object : DelegatingNamedResolver("failing", { EMPTY_RESOLVER }) {
      override fun resolveClass(className: BinaryClassName): ResolutionResult<ClassNode> = throw failure
    }
    val resolver = CacheResolver(delegate)

    try {
      resolver.resolveClass("Class")
      fail("Expected class resolution to fail")
    } catch (e: CompletionException) {
      assertSame(failure, e.cause)
    }
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

    assertEquals(setOf("some/package"), resolver.packages)
    assertEquals(setOf(sameClass, class1, class2), resolver.allClassNames)

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
