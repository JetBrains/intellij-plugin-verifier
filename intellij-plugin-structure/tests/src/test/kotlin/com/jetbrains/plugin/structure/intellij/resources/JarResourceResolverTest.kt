package com.jetbrains.plugin.structure.intellij.resources

import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.base.utils.withZipFsSeparator
import com.jetbrains.plugin.structure.classes.resolvers.jar.initializeSampleJarContent
import com.jetbrains.plugin.structure.intellij.plugin.JarFilesResourceResolver
import com.jetbrains.plugin.structure.jar.CachingJarFileSystemProvider
import com.jetbrains.plugin.structure.jar.DefaultJarFileSystemProvider
import com.jetbrains.plugin.structure.jar.FsHandleFileSystem
import com.jetbrains.plugin.structure.jar.JarFileSystemProvider
import junit.framework.TestCase.assertTrue
import net.bytebuddy.ByteBuddy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.FileSystem
import java.nio.file.Path
import java.util.*

class JarResourceResolverTest {
  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  private lateinit var byteBuddy: ByteBuddy

  private val fileSystemProvider = DefaultJarFileSystemProvider()

  lateinit var simpleJarPath: Path

  lateinit var complexJarPath: Path

  @Before
  fun setUp() {
    byteBuddy = ByteBuddy()
    simpleJarPath = buildZipFile(randomJarPath()) {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin />
          """.trimIndent()
        }
        file("nested.xml") {
          """
            <idea-plugin />
          """.trimIndent()
        }
      }
      file("module.xml") {
        """
            <idea-plugin />
          """.trimIndent()
      }
    }
    complexJarPath = initializeSampleJarContent(randomJarPath(), byteBuddy)
  }

  @Test
  fun `resource is resolved`() {
    val resolver = JarFilesResourceResolver(listOf(complexJarPath, simpleJarPath))
    assertResolverIsCorrect(resolver)
  }

  @Test
  fun `alternative implementation of resolver is resolved`() {
    val resolver = JarsResourceResolver(listOf(complexJarPath, simpleJarPath), fileSystemProvider)
    assertResolverIsCorrect(resolver)
  }

  @Test
  fun `resolved jar entries are cached`() {
    val countingProvider = CountingJarFileSystemProvider(DefaultJarFileSystemProvider())
    val resolver = JarsResourceResolver(listOf(complexJarPath, simpleJarPath), countingProvider)

    repeat(2) {
      val result = resolver.resolveResource("module.xml", Path.of("/"))
      assertTrue(result is ResourceResolver.Result.Found)
      (result as ResourceResolver.Result.Found).use {
        assertEquals("module.xml", it.path.toString().withZipFsSeparator())
      }
    }

    assertEquals(3, countingProvider.requestCount)
  }

  @Test
  fun `resolved jar paths can resolve siblings`() {
    val resolver = JarsResourceResolver(listOf(simpleJarPath), fileSystemProvider)

    val baseResult = resolver.resolveResource("META-INF/plugin.xml", Path.of("/"))
    assertTrue(baseResult is ResourceResolver.Result.Found)

    (baseResult as ResourceResolver.Result.Found).use {
      val siblingResult = resolver.resolveResource("nested.xml", it.path)
      assertTrue(siblingResult is ResourceResolver.Result.Found)
      (siblingResult as ResourceResolver.Result.Found).use { sibling ->
        assertEquals("META-INF/nested.xml", sibling.path.toString().withZipFsSeparator())
      }
    }
  }

  @Test
  fun `missing jar entries close cached filesystem handles`() {
    val cachingProvider = CachingJarFileSystemProvider(retentionTimeInSeconds = Long.MAX_VALUE)
    val resolver = JarsResourceResolver(listOf(simpleJarPath), cachingProvider)
    val fs = cachingProvider.getFileSystem(simpleJarPath) as FsHandleFileSystem
    val delegate = fs.delegateFileSystem
    fs.close()

    repeat(2) {
      val result = resolver.resolveResource("missing.xml", Path.of("/"))
      assertTrue(result is ResourceResolver.Result.NotFound)
    }

    cachingProvider.close()
    assertFalse(delegate.isOpen)
  }

  private fun assertResolverIsCorrect(resolver: ResourceResolver) {
    with(resolver.resolveResource("META-INF/plugin.xml", Path.of("/"))) {
      assertTrue(this is ResourceResolver.Result.Found)
      this as ResourceResolver.Result.Found
      assertEquals("META-INF/plugin.xml", path.toString().withZipFsSeparator())
    }

    with(resolver.resolveResource("META-INF/plugin.xml", Path.of("META-INF"))) {
      assertTrue(this is ResourceResolver.Result.Found)
      this as ResourceResolver.Result.Found
      assertEquals("META-INF/plugin.xml", path.toString().withZipFsSeparator())
    }

    with(resolver.resolveResource("services/java.nio.file.spi.FileSystemProvider", Path.of("META-INF/services/"))) {
      assertTrue(this is ResourceResolver.Result.Found)
      this as ResourceResolver.Result.Found
      assertEquals("META-INF/services/java.nio.file.spi.FileSystemProvider", path.toString().withZipFsSeparator())
    }
  }

  private fun randomJarPath(): Path {
    val jarSuffix = UUID.randomUUID().toString()
    return temporaryFolder.newFile("classes-$jarSuffix.jar").toPath()
  }

  private class CountingJarFileSystemProvider(private val delegate: JarFileSystemProvider) : JarFileSystemProvider {
    var requestCount = 0

    override fun getFileSystem(jarPath: Path): FileSystem {
      requestCount++
      return delegate.getFileSystem(jarPath)
    }
  }
}
