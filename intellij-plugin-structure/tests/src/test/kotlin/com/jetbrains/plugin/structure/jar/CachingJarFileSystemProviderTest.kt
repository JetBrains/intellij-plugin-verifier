package com.jetbrains.plugin.structure.jar

import com.jetbrains.plugin.structure.base.fs.isClosed
import com.jetbrains.plugin.structure.base.utils.exists
import com.jetbrains.plugin.structure.base.utils.isFile
import com.jetbrains.plugin.structure.base.utils.writeText
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.net.URI
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Path

class CachingJarFileSystemProviderTest {

  @Rule
  @JvmField
  val tempFolder = TemporaryFolder()

  private lateinit var jarPath: Path

  @Before
  fun setUp() {
    jarPath = tempFolder.root.toPath().resolve("test.jar")
    FileSystems.newFileSystem(URI.create("jar:${jarPath.toUri()}"), mapOf<String, Any>("create" to true)).use {
      it.getPath("hello.txt").writeText("Hello World")
    }
  }

  @Test
  fun `retrieve path even if the underlying filesystem is closed`() {
    val fileSystemProvider = CachingJarFileSystemProvider()
    val fs = fileSystemProvider.getFileSystem(jarPath)
    val helloTxtPath: Path = fs.getPath("hello.txt")
    assertTrue(helloTxtPath.exists())
    assertTrue(helloTxtPath.isFile)
    fs.close()

    assertFalse(fs.isOpen)

    // under the cover, the filesystem has been reopened
    val anotherHelloWorldPath: Path = fs.getPath("hello.txt")
    assertTrue(anotherHelloWorldPath.exists())
  }

  @Test
  fun `path is retrieved from the filesystem that is manually closed`() {
    val fileSystemProvider = CachingJarFileSystemProvider(retentionTimeInSeconds = Long.MAX_VALUE)
    val fs = fileSystemProvider.getFileSystem(jarPath)
    val helloTxtPath: Path = fs.getPath("hello.txt")
    assertTrue(helloTxtPath.exists())
    assertTrue(helloTxtPath.isFile)
    fs.close()

    assertFalse(fs.isOpen)

    val anotherHelloTxtPath: Path = fs.getPath("hello.txt")
    assertTrue(anotherHelloTxtPath.exists())

    val anotherFs = fileSystemProvider.getFileSystem(jarPath)
    val helloTxtFromAnotherFs: Path = anotherFs.getPath("hello.txt")
    assertTrue(helloTxtFromAnotherFs.exists())
    assertTrue(helloTxtFromAnotherFs.isFile)
  }

  @Test
  fun `two filesystems are retrieved for the same path`() {
    val fileSystemProvider = CachingJarFileSystemProvider(retentionTimeInSeconds = Long.MAX_VALUE)
    val fs = fileSystemProvider.getFileSystem(jarPath)
    val anotherFs = fileSystemProvider.getFileSystem(jarPath)

    assertTrue((anotherFs as? FsHandleFileSystem)?.hasSameDelegate(fs) == true)

    fs.close()
    // still 1 reference left open
    assertTrue(fs.isOpen)
    assertTrue(anotherFs.isOpen)

    anotherFs.close()
    assertFalse(anotherFs.isOpen)

    val yetAnotherFs = fileSystemProvider.getFileSystem(jarPath)
    assertTrue(yetAnotherFs !== fs)
    assertTrue(yetAnotherFs !== anotherFs)
    assertTrue(yetAnotherFs.isOpen)
    assertTrue((yetAnotherFs as? FsHandleFileSystem)?.delegateFileSystem?.isOpen == true)
  }

  @Test
  fun `two filesystems are closed in reverse order of retrieval`() {
    val fileSystemProvider = CachingJarFileSystemProvider(retentionTimeInSeconds = Long.MAX_VALUE)
    val fs = fileSystemProvider.getFileSystem(jarPath)
    val anotherFs = fileSystemProvider.getFileSystem(jarPath)
    assertTrue(fs.isOpen)
    assertTrue(anotherFs.isOpen)

    anotherFs.close()
    // still one delegate left open (one from `fs`)
    assertTrue(fs.isOpen)
    fs.close()

    // no clients left, underlying FS delegate is closed
    assertFalse(fs.isOpen)
    assertFalse(anotherFs.isOpen)
  }

  @Test
  fun `underlying filesystem is closed despite the usage`() {
    val fileSystemProvider = CachingJarFileSystemProvider(retentionTimeInSeconds = Long.MAX_VALUE)
    val fs = fileSystemProvider.getFileSystem(jarPath)
    fs as FsHandleFileSystem
    assertTrue(fs.isOpen)
    fs.initialDelegateFileSystem.close()
    // delegate is closed, but the wrapper will reopen it
    assertTrue(fs.isOpen)

    val fs1 = fileSystemProvider.getFileSystem(jarPath)
    assertSame(fs, fs1)
    assertTrue(fs1.isOpen)
    assertTrue(fs.hasSameDelegate(fs1))
  }

  @Test
  fun `closing of filesystem is idempotent`() {
    val fileSystemProvider = CachingJarFileSystemProvider(retentionTimeInSeconds = Long.MAX_VALUE)
    val fs = fileSystemProvider.getFileSystem(jarPath)
    fs as FsHandleFileSystem
    assertTrue(fs.isOpen)

    // standard close
    fs.close()
    assertTrue(fs.isClosed)
    // second close is expected to be idempotent
    fs.close()
    assertTrue(fs.isClosed)
  }

  @Test
  fun `JAR URIs are normalized`() {
    val jarPath = tempFolder.root.toPath().resolve("test.jar")
    FileSystems.newFileSystem(URI.create("jar:${jarPath.toUri()}"), mapOf<String, Any>("create" to true)).use {
      it.getPath("hello.txt").writeText("Hello World")
    }

    val fileSystemProvider = CachingJarFileSystemProvider(retentionTimeInSeconds = Long.MAX_VALUE)

    val anotherJarPath = Path.of("${tempFolder.root.absolutePath}/../${tempFolder.root.name}/test.jar")

    // Getting two filesystems with different paths that point to the same file.
    val fs1 = fileSystemProvider.getFileSystem(jarPath)
    val fs2 = fileSystemProvider.getFileSystem(anotherJarPath)

    fs1.close()

    // delegate is open, and out of 2 references, only 1 is closed.
    assertTrue(fs1.isOpen)
    // The second FS should be still open, as there is still 1 client accessing it.
    assertTrue(fs2.isOpen)

    // second close of the 1st filesystem should decrease the reference count to 0, closing the instance
    fs1.close()

    assertFalse(fs1.isOpen)
    assertFalse(fs2.isOpen)

    // let's close the 2nd filesystem as well, but since there are no references left, this is a double close.
    // double close is an idempotent operation.
    fs2.close()

    assertFalse(fs1.isOpen)
    assertFalse(fs2.isOpen)

    val anotherFs1 = fileSystemProvider.getFileSystem(jarPath)
    assertTrue(anotherFs1.isOpen)
    assertFalse(fs1 == anotherFs1)

    val yetAnotherFs1 = fileSystemProvider.getFileSystem(jarPath)
    assertTrue(yetAnotherFs1.isOpen)
    assertTrue(anotherFs1.hasSameDelegate(yetAnotherFs1))

    val anotherFs2 = fileSystemProvider.getFileSystem(anotherJarPath)
    assertTrue(anotherFs2.isOpen)
    assertFalse(fs2 == anotherFs2)

    val yetAnotherFs2 = fileSystemProvider.getFileSystem(anotherJarPath)
    assertTrue(yetAnotherFs2.isOpen)
    assertTrue(anotherFs2.hasSameDelegate(yetAnotherFs2))
  }

  private fun FileSystem.hasSameDelegate(fs: FileSystem): Boolean {
    return (this as? FsHandleFileSystem)?.hasSameDelegate(fs) == true
  }
}