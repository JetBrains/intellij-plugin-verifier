package com.jetbrains.plugin.structure.classes.resolvers.jar

import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.base.utils.emptyClass
import com.jetbrains.plugin.structure.jar.CachingJarFileSystemProvider
import com.jetbrains.plugin.structure.jar.DefaultJarFileSystemProvider
import com.jetbrains.plugin.structure.jar.FsHandleFileSystem
import com.jetbrains.plugin.structure.jar.Jar
import com.jetbrains.plugin.structure.jar.JarArchiveException
import com.jetbrains.plugin.structure.jar.JarEntryResolver
import com.jetbrains.plugin.structure.jar.JarFileSystemProvider
import com.jetbrains.plugin.structure.jar.SingletonCachingJarFileSystemProvider
import com.jetbrains.plugin.structure.jar.descriptors.DescriptorReference
import net.bytebuddy.ByteBuddy
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

class JarTest {
  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  private lateinit var byteBuddy: ByteBuddy

  @Before
  fun setUp() {
    byteBuddy = ByteBuddy()
  }

  private fun createJar(jarPath: Path, jarFileSystemProvider: JarFileSystemProvider = SingletonCachingJarFileSystemProvider): Jar {
    buildZipFile(jarPath) {
      file("intellij.example.xml", "<idea-plugin />")
      dir("META-INF") {
        file("plugin.properties") {
          """
            name=My Class
            enabled=1
          """.trimIndent()
        }
        file("pluginIcon.svg", "<svg width='1' height='1' xmlns='http://www.w3.org/2000/svg' />")
        dir("services") {
          file("kotlinx.coroutines.internal.MainDispatcherFactory") {
            "com.intellij.openapi.application.impl.EdtCoroutineDispatcherFactory"
          }
          file("java.nio.file.spi.FileSystemProvider") {
            "com.intellij.platform.ijent.community.impl.nio.IjentNioFileSystemProvider"
          }
          file("org.freedesktop.dbus.spi.transport.ITransportProvider") {
            """
              # Native
              org.freedesktop.dbus.transport.jre.NativeTransportProvider
              # TCP
              org.freedesktop.dbus.transport.tcp.TCPTransportProvider
            """.trimIndent()
          }
        }
        file("plugin.xml", "<idea-plugin />")
      }
      dirs("com/example") {
        file("MyClass.class", byteBuddy.emptyClass("com.example.MyClass"))
        file("MyClass.properties") {
          """
            mode=simple
            type=other
          """.trimIndent()
        }
        file("MyClass_en_US.properties") {
          """
            mode=i14n
            type=localized
            lang=en_US
          """.trimIndent()
        }
        dir("impl") {
          file("MyImpl.class", byteBuddy.emptyClass("com.example.impl.MyImpl"))
        }
      }
    }

    val jar = Jar(jarPath, jarFileSystemProvider)
    jar.init()
    return jar
  }

  @Test
  fun `JAR is scanned`() {
    createJar(temporaryFolder.newFile("plugin.jar").toPath()).use { jar ->
      with(jar.classes) {
        assertEquals(2, size)
        assertTrue(contains("com/example/MyClass"))
        assertTrue(contains("com/example/impl/MyImpl"))
      }
      with(jar.bundleNames) {
        assertEquals(2, size)
        assertTrue(contains("com.example.MyClass"))
        assertTrue(contains("META-INF.plugin"))
      }
      with(jar.packages.all) {
        assertEquals(3, size)
        assertTrue(contains("com"))
        assertTrue(contains("com/example"))
        assertTrue(contains("com/example/impl"))
      }
      assertTrue(jar.containsPackage("com"))
      assertTrue(jar.containsPackage("com/example"))
      assertTrue(jar.containsPackage("com/example/impl"))

      with(jar.serviceProviders) {
        assertEquals(3, size)
        val fileSystemProviderSp = this["java.nio.file.spi.FileSystemProvider"]
        assertNotNull(fileSystemProviderSp)
        fileSystemProviderSp!!
        assertEquals(1, fileSystemProviderSp.size)
        assertEquals(
          "com.intellij.platform.ijent.community.impl.nio.IjentNioFileSystemProvider",
          fileSystemProviderSp.single()
        )

        val mainDispatcherFactorySp = this["kotlinx.coroutines.internal.MainDispatcherFactory"]
        assertNotNull(mainDispatcherFactorySp)
        mainDispatcherFactorySp!!
        assertEquals(1, mainDispatcherFactorySp.size)
        assertEquals(
          "com.intellij.openapi.application.impl.EdtCoroutineDispatcherFactory",
          mainDispatcherFactorySp.single()
        )

        val itTransportProvider = this["org.freedesktop.dbus.spi.transport.ITransportProvider"]
        assertNotNull(itTransportProvider)
        itTransportProvider!!
        assertEquals(2, itTransportProvider.size)
        assertTrue(itTransportProvider.contains("org.freedesktop.dbus.transport.jre.NativeTransportProvider"))
        assertTrue(itTransportProvider.contains("org.freedesktop.dbus.transport.tcp.TCPTransportProvider"))
      }

      with(jar.descriptorCandidates) {
        assertEquals(2, size)
        val descriptorCandidatePaths =
          filterIsInstance<DescriptorReference>()
            .mapTo(mutableSetOf()) { it.path.toString() }
        assertEquals(setOf("intellij.example.xml", "META-INF/plugin.xml"), descriptorCandidatePaths)
      }
    }
  }

  @Test
  fun `malformed JAR is handled`() {
    val jarPath = temporaryFolder.newFile("plugin-malformed.jar").toPath()
    Files.write(jarPath, ByteArray(1))

    assertThrows(JarArchiveException::class.java) {
      Jar(jarPath, SingletonCachingJarFileSystemProvider).init()
    }
  }

  @Test
  fun `extra entry resolver is applied`() {
    val jarPath = temporaryFolder.newFile("plugin-extra-entry-resolver.jar").toPath()
    createJar(jarPath).use { jar ->
      val pluginIconResolver = PluginIconJarEntryResolver()
      val entryResolvers: List<JarEntryResolver<*>> = listOf(pluginIconResolver)
      val jar = Jar(jarPath, SingletonCachingJarFileSystemProvider, entryResolvers)
      jar.init()

      assertEquals(listOf("pluginIcon.svg"), jar.entryResolverResults[pluginIconResolver.key])
    }
  }

  @Test
  fun `all classes are processed`() {
    createJar(temporaryFolder.newFile("plugin-class-processing.jar").toPath()).use { jar ->
      val classes = mutableListOf<String>()
      jar.processAllClasses { name, _ ->
        classes += name
        true
      }
      val expectedClasses = setOf(
        "com/example/MyClass",
        "com/example/impl/MyImpl",
      )
      assertEquals(expectedClasses, classes.toSet())
    }
  }

  @Test
  fun `all classes are processed when underlying filesystem is closed`() {
    var reopenBecauseNull = 0
    var reopenBecauseClosed = 0

    val fsProvider = object : JarFileSystemProvider {
      private val defaultFsProvider = DefaultJarFileSystemProvider()

      var filesystem: FileSystem? = null

      override fun getFileSystem(jarPath: Path): FileSystem {
        return if (filesystem == null) {
          defaultFsProvider.getFileSystem(jarPath).also {
            filesystem = it
            reopenBecauseNull++
          }
        } else if (filesystem?.isOpen == true) {
          filesystem!!
        } else {
          defaultFsProvider.getFileSystem(jarPath).also {
            filesystem = it
            reopenBecauseClosed++
          }
        }
      }
    }
    createJar(temporaryFolder.newFile("plugin-class-processing.jar").toPath(), fsProvider).use { jar ->
      // Initialize Service Providers via FileSystem
      jar.serviceProviders

      val fs = fsProvider.filesystem

      var iteration = 0
      jar.processAllClasses { name, paths ->
        if (iteration == 1) {
          fs?.close()
        }
        iteration++
        true
      }
    }
    assertEquals(1, reopenBecauseNull)
    assertEquals(1, reopenBecauseClosed)
  }

  @Test
  fun `all classes are processed when underlying filesystem is closed with caching filesystem provider`() {
    val fsProvider = CachingJarFileSystemProvider(retentionTimeInSeconds = Long.MAX_VALUE)
    val jarPath = temporaryFolder.newFile("plugin-class-processing.jar").toPath()
    createJar(jarPath, fsProvider).use { jar ->
      val uniqueFileSystems = IdentityHashMap<FileSystem, Unit>()
      // Initialize Service Providers via FileSystem
      jar.serviceProviders

      var iteration = 0
      jar.processAllClasses { name, paths ->
        if (iteration == 1) {
          val fs = fsProvider.getFileSystem(jarPath)
          assertTrue(fs is FsHandleFileSystem)
          fs as FsHandleFileSystem
          uniqueFileSystems[fs] = Unit
          fs.delegate.close()
        }
        iteration++
        true
      }
      // Provider should have closed 1 filesystem (in the 1st iteration)
      assertEquals(1, uniqueFileSystems.size)
    }
  }
}