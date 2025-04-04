package com.jetbrains.plugin.structure.jar

import com.jetbrains.plugin.structure.base.utils.writeText
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Path

class SingletonCachingJarFileSystemProviderTest {

  @Rule
  @JvmField
  val tempFolder = TemporaryFolder()

  @Test
  fun `Jar Uris are normalized`() {
    val jarPath = tempFolder.root.toPath().resolve("test.jar")
    FileSystems.newFileSystem(URI.create("jar:${jarPath.toUri()}"), mapOf<String, Any>("create" to true)).use {
      it.getPath("hello.txt").writeText("Hello World")
    }

    val fileSystemProvider = CachingJarFileSystemProvider(retentionTimeInSeconds = 0)

    //Getting two filesystems with different path that point to the same file.
    val fs1 = fileSystemProvider.getFileSystem(jarPath)
    val fs2 = fileSystemProvider.getFileSystem(
      Path.of("${tempFolder.root.absolutePath}/../${tempFolder.root.name}/test.jar")
    )

    Assert.assertSame(fs1, fs2)

    //After jar is closed for the first time, filesystem remains open
    fileSystemProvider.close(jarPath)
    Assert.assertTrue(fs1.isOpen)
    Assert.assertTrue(fs2.isOpen)

    //After closed for second time, the filesystem has no users and is closed
    fileSystemProvider.close(jarPath)
    Assert.assertFalse(fs1.isOpen)
    Assert.assertFalse(fs2.isOpen)
  }
}