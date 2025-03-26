package com.jetbrains.plugin.structure.classes.resolvers.jar

import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.jar.SingletonCachingJarFileSystemProvider
import net.bytebuddy.ByteBuddy
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Path

class JarTest {
  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  private lateinit var byteBuddy: ByteBuddy

  @Before
  fun setUp() {
    byteBuddy = ByteBuddy()
  }

  private fun createJar(jarPath: Path): Jar {
    buildZipFile(jarPath) {
      dir("META-INF") {
        file("plugin.properties") {
          """
            name=My Class
            enabled=1
          """.trimIndent()
        }
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
      }
      dirs("com/example") {
        file("MyClass.class", emptyClass("com.example.MyClass"))
        file("MyClass.properties") {
          """
            mode=simple
            type=other
          """.trimIndent()
        }
        dir("impl") {
          file("MyImpl.class", emptyClass("com.example.impl.MyImpl"))
        }
      }
    }

    val jar = Jar(jarPath, SingletonCachingJarFileSystemProvider)
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
      with(jar.packages) {
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
    }
  }

  @Test
  fun `all classes are processed`() {
    createJar(temporaryFolder.newFile("plugin-class-processing.jar").toPath()).use { jar ->
      val classes = mutableListOf<String>()
      jar.processAllClasses {
        classes += it.name
        true
      }
      val expectedClasses = setOf(
        "com/example/MyClass",
        "com/example/impl/MyImpl",
      )
      assertEquals(expectedClasses, classes.toSet())
    }
  }

  // FIXME duplicate code
  private fun emptyClass(fullyQualifiedName: String): ByteArray {
    return byteBuddy
      .subclass(Object::class.java)
      .name(fullyQualifiedName)
      .make()
      .bytes
  }
}