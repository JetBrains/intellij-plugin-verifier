package com.jetbrains.plugin.structure.classes.resolvers.jar

import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.base.utils.emptyClass
import com.jetbrains.plugin.structure.classes.resolvers.LazyJarResolver
import com.jetbrains.plugin.structure.jar.Jar
import com.jetbrains.plugin.structure.jar.JarFileSystemProvider
import net.bytebuddy.ByteBuddy
import org.junit.Assert.assertTrue
import java.nio.file.Path

internal fun initializeSampleJarContent(jarPath: Path, byteBuddy: ByteBuddy): Path = with(byteBuddy) {
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
      file("MyClass_en_US.properties") {
        """
            mode=i14n
            type=localized
            lang=en_US
          """.trimIndent()
      }
      dir("impl") {
        file("MyImpl.class", emptyClass("com.example.impl.MyImpl"))
      }
    }
  }
}


internal fun initializeSampleJar(jarPath: Path, byteBuddy: ByteBuddy, jarFileSystemProvider: JarFileSystemProvider): Jar = with(byteBuddy) {
  return Jar(jarPath, jarFileSystemProvider).apply { init() }
}

/**
 * Returns the [Jar] that this resolver has already initialized; reading it must not trigger the initialization itself.
 */
internal fun LazyJarResolver.initializedJar(): Jar {
  val jar = LazyJarResolver::class.java.getDeclaredField("jar\$delegate").apply { isAccessible = true }.get(this) as Lazy<*>
  assertTrue("JAR of resolver '$name' must be initialized", jar.isInitialized())
  return jar.value as Jar
}