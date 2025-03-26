package com.jetbrains.plugin.structure.classes.resolvers.jar

import com.jetbrains.plugin.structure.classes.resolvers.FileOrigin
import com.jetbrains.plugin.structure.classes.resolvers.LazyJarResolver
import com.jetbrains.plugin.structure.classes.resolvers.ResolutionResult
import com.jetbrains.plugin.structure.classes.resolvers.ResolutionResult.Found
import com.jetbrains.plugin.structure.classes.resolvers.ResolutionResult.NotFound
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.jar.DefaultJarFileSystemProvider
import junit.framework.TestCase.assertTrue
import net.bytebuddy.ByteBuddy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.objectweb.asm.tree.ClassNode
import java.nio.file.Path
import java.util.*

class LazyJarResolverTest {
  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  private lateinit var byteBuddy: ByteBuddy

  private val fileOrigin = object : FileOrigin {
    override val parent: FileOrigin? = null
  }

  private lateinit var resolver: Resolver

  @Before
  fun setUp() {
    byteBuddy = ByteBuddy()
    // FIXME FS-closing issues with Singleton
    val fsProvider = DefaultJarFileSystemProvider()
    val jarPath = initializeSampleJarContent(randomJarPath(), byteBuddy)
    resolver = LazyJarResolver(
      jarPath, Resolver.ReadMode.FULL, fileOrigin,
      fsProvider
    )
  }

  @Test
  fun `classes are resolved`() {
    val comExampleMyClassBinaryName = "com/example/MyClass"
    val comExampleMyClass = resolver.resolveClass(comExampleMyClassBinaryName)
    assertTrue(comExampleMyClass is Found)
    comExampleMyClass as Found
    assertEquals(comExampleMyClassBinaryName, comExampleMyClass.value.name)
  }

  @Test
  fun `nonexistent class is resolved`() {
    val missingClass = resolver.resolveClass("com/nonexistent/MissingClass")
    assertTrue(missingClass is NotFound)
  }

  @Test
  fun `class is present`() {
    val comExampleMyClassBinaryName = "com/example/MyClass"
    assertTrue(resolver.containsClass(comExampleMyClassBinaryName))
  }

  @Test
  fun `nonexistent class is not in the resolver`() {
    assertFalse(resolver.containsClass("com/nonexistent/MissingClass"))
  }

  @Test
  fun `all classes are processesd`() {
    val classes = mutableListOf<String>()
    val failures = mutableListOf<String>()
    resolver.processAllClasses {
      when (it) {
        is Found<ClassNode> -> classes += it.value.name
        is ResolutionResult.FailedToRead -> failures += it.reason
        is ResolutionResult.Invalid -> failures += it.message
        NotFound -> failures += "not found"
      }
      true
    }
    assertTrue(failures.isEmpty())
    assertEquals(listOf("com/example/MyClass", "com/example/impl/MyImpl"), classes)
  }

  @Test
  fun `exact property resource bundle`() {
    val resolution = resolver.resolveExactPropertyResourceBundle("com.example.MyClass", Locale.US)
    assertTrue(resolution is Found)
    resolution as Found
    val bundle = resolution.value
    assertEquals(3, bundle.keys.toList().size)
  }

  private fun randomJarPath(): Path {
    val jarSuffix = UUID.randomUUID().toString()
    return temporaryFolder.newFile("classes-$jarSuffix.jar").toPath()
  }

  @After
  fun tearDown() {
    resolver.close()
  }
}