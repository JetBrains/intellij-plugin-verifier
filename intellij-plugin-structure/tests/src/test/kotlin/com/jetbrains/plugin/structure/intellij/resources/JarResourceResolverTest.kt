package com.jetbrains.plugin.structure.intellij.resources

import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.base.utils.withZipFsSeparator
import com.jetbrains.plugin.structure.classes.resolvers.jar.initializeSampleJarContent
import com.jetbrains.plugin.structure.intellij.plugin.JarFilesResourceResolver
import com.jetbrains.plugin.structure.jar.JarFileSystemProvider
import com.jetbrains.plugin.structure.jar.DefaultJarFileSystemProvider
import junit.framework.TestCase.assertTrue
import net.bytebuddy.ByteBuddy
import org.junit.Assert.assertEquals
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
  fun `alternative implementation caches resolved jar owner for repeated lookups`() {
    val countingProvider = CountingJarFileSystemProvider(DefaultJarFileSystemProvider())
    val resolver = JarsResourceResolver(listOf(complexJarPath, simpleJarPath), countingProvider)

    resolver.resolveResource("META-INF/plugin.xml", Path.of("META-INF"))
    resolver.resolveResource("META-INF/plugin.xml", Path.of("META-INF"))

    assertEquals(3, countingProvider.getFileSystemCalls)
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

  private class CountingJarFileSystemProvider(
    private val delegate: JarFileSystemProvider,
  ) : JarFileSystemProvider {
    var getFileSystemCalls = 0
      private set

    override fun getFileSystem(jarPath: Path): FileSystem {
      getFileSystemCalls += 1
      return delegate.getFileSystem(jarPath)
    }

    override fun getFileSystem(jarPath: Path, configuration: JarFileSystemProvider.Configuration): FileSystem {
      getFileSystemCalls += 1
      return delegate.getFileSystem(jarPath, configuration)
    }
  }
}
