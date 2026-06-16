/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.jar

import com.github.benmanes.caffeine.cache.Cache
import com.jetbrains.plugin.structure.base.fs.isClosed
import com.jetbrains.plugin.structure.base.utils.exists
import com.jetbrains.plugin.structure.base.utils.isFile
import com.jetbrains.plugin.structure.base.utils.writeText
import com.jetbrains.plugin.structure.fs.FsHandlerFileSystemProvider
import com.jetbrains.plugin.structure.fs.FsHandlerPath
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.net.URI
import java.nio.ByteBuffer
import java.nio.channels.SeekableByteChannel
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileAttribute
import java.nio.file.attribute.FileAttributeView
import java.nio.file.spi.FileSystemProvider
import java.util.concurrent.atomic.AtomicInteger

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
    assertSame(fs, yetAnotherFs)
    assertSame(anotherFs, yetAnotherFs)
    assertTrue(yetAnotherFs.isOpen)
    assertTrue((yetAnotherFs as? FsHandleFileSystem)?.delegateFileSystem?.isOpen == true)
  }

  @Test
  fun `two filesystems are closed in reverse order of retrieval`() {
    val fileSystemProvider = CachingJarFileSystemProvider(retentionTimeInSeconds = Long.MAX_VALUE)
    val fs = fileSystemProvider.getFileSystem(jarPath) as FsHandleFileSystem
    val anotherFs = fileSystemProvider.getFileSystem(jarPath) as FsHandleFileSystem
    val underlying = fs.delegateFileSystem
    assertTrue(fs.isOpen)
    assertTrue(anotherFs.isOpen)
    assertSame(fs, anotherFs)

    anotherFs.close()
    // still one delegate left open (one from `fs`)
    assertTrue(fs.isOpen)
    assertTrue(underlying.isOpen)
    fs.close()

    // no clients left, the cached handle is closed for clients, but the delegate remains cached
    assertFalse(fs.isOpen)
    assertFalse(anotherFs.isOpen)
    assertTrue(underlying.isOpen)

    fileSystemProvider.close()
    assertFalse(underlying.isOpen)
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
    assertSame(fs1, anotherFs1)

    val yetAnotherFs1 = fileSystemProvider.getFileSystem(jarPath)
    assertTrue(yetAnotherFs1.isOpen)
    assertSame(anotherFs1, yetAnotherFs1)
    assertTrue(anotherFs1.hasSameDelegate(yetAnotherFs1))

    val anotherFs2 = fileSystemProvider.getFileSystem(anotherJarPath)
    assertTrue(anotherFs2.isOpen)
    assertSame(fs2, anotherFs2)

    val yetAnotherFs2 = fileSystemProvider.getFileSystem(anotherJarPath)
    assertTrue(yetAnotherFs2.isOpen)
    assertSame(anotherFs2, yetAnotherFs2)
    assertTrue(anotherFs2.hasSameDelegate(yetAnotherFs2))
  }

  @Test
  fun `path used after cache eviction surfaces as ClosedFileSystemException, not StackOverflowError`() {
    val fileSystemProvider = CachingJarFileSystemProvider(retentionTimeInSeconds = Long.MAX_VALUE)
    val fs = fileSystemProvider.getFileSystem(jarPath) as FsHandleFileSystem
    val helloTxtPath: Path = fs.getPath("hello.txt")

    // Simulate Caffeine evicting the handle while a Path to it is still held: this drops
    // referenceCount to -1, after which getOrReopenDelegateFileSystem refuses to reopen.
    fs.close()
    fs.onCacheRemoval()
    assertTrue(fs.isClosed)

    try {
      Files.readAttributes(helloTxtPath, BasicFileAttributes::class.java)
      fail("Expected ClosedFileSystemException")
    } catch (_: ClosedFileSystemException) {
      // expected — before the depth bound this looped in `unwrapped` until StackOverflowError.
    }
  }

  private fun FileSystem.hasSameDelegate(fs: FileSystem): Boolean {
    return (this as? FsHandleFileSystem)?.hasSameDelegate(fs) == true
  }

  // Regression: MP-7468. newDirectoryStream used to return raw ZipPath entries, so any
  // downstream Files.* call on them bypassed FsHandleFileSystem's reopen-on-close safety net.
  @Test
  fun `newDirectoryStream entries are FsHandlerPath and recover after a delegate close`() {
    val fileSystemProvider = CachingJarFileSystemProvider(retentionTimeInSeconds = Long.MAX_VALUE)
    val fs = fileSystemProvider.getFileSystem(jarPath) as FsHandleFileSystem
    val root = fs.getPath("/")

    val entries = Files.newDirectoryStream(root).use { it.toList() }
    assertTrue("expected at least one entry in the JAR root", entries.isNotEmpty())
    entries.forEach { assertTrue("entry $it is not wrapped: ${it::class}", it is FsHandlerPath) }

    // Simulate Caffeine evicting the cached ZipFileSystem after entries were collected.
    fs.initialDelegateFileSystem.close()

    val hello = entries.first { it.fileName.toString() == "hello.txt" }
    // Routes through FsHandlerFileSystemProvider.readAttributes -> unwrapped -> reopen.
    assertTrue(Files.isRegularFile(hello))
  }

  // Regression: MP-8146. The JVM caches ZipFileSystems globally, so two FsHandleFileSystem
  // wrappers created for the same JAR at different times share the same underlying javaNioFs ZipFileSystem.
  // A javaNioFs-level ref-count must gate javaNioFs.close() so that javaNioFs stays open until every wrapper
  // that holds it has been evicted.
  @Test
  fun `raw delegate javaNioFs stays open until its wrapper is evicted`() {
    val fileSystemProvider = CachingJarFileSystemProvider(retentionTimeInSeconds = Long.MAX_VALUE)

    val fs = fileSystemProvider.getFileSystem(jarPath) as FsHandleFileSystem
    val javaNioFs = fs.initialDelegateFileSystem

    // Close fs's client reference so referenceCount drops to 0 (idle in cache).
    fs.close()
    assertFalse(fs.isOpen)
    // Raw javaNioFs must still be open — it is still tracked by the javaNioFs-level ref-count.
    assertTrue("raw java.nio.file.FileSystem must not close while its wrapper is still registered in the cache", javaNioFs.isOpen)

    // Simulate Caffeine evicting 'fs' instance. This fires onCacheRemoval → closeDelegate → releaseDelegate.
    // raw javaNioFs has no other holders, so it should now close.
    fs.onCacheRemoval()
    assertFalse("raw java.nio.file.FileSystem must close after the last wrapper is evicted", javaNioFs.isOpen)
    fileSystemProvider.close()
  }

  /**
   * Regression coverage for the actual MP-8146 shape:
   * one cached wrapper is evicted while it is still in use, and the next lookup creates
   * a second wrapper around the same JVM-cached ZipFileSystem delegate.
   */
  @Test
  fun `shared delegate Z stays open while one of multiple wrappers still holds it`() {
    val fileSystemProvider = CachingJarFileSystemProvider(retentionTimeInSeconds = Long.MAX_VALUE)

    val fs1 = fileSystemProvider.getFileSystem(jarPath) as FsHandleFileSystem
    val rawFs = fs1.initialDelegateFileSystem

    // Evict the cache entry without closing fs1. This mirrors Caffeine removing an entry
    // while a client still owns a reference, so fs1 is removed but remains usable.
    fileSystemProvider.evictCachedHandle(jarPath)

    // The JVM-level ZipFileSystem cache returns the same delegate 'rawFs' for the replacement
    // wrapper. This is the case that needs delegate-level reference counting.
    val fs2 = fileSystemProvider.getFileSystem(jarPath) as FsHandleFileSystem
    assertNotSame("fs1 and fs2 must be distinct wrapper instances", fs1, fs2)
    assertSame("fs1 and fs2 must share the same underlying raw filesystem rawFs", rawFs, fs2.initialDelegateFileSystem)

    fs1.close()
    assertTrue("Raw filesystem rawFs must stay open while fs2 still holds it", rawFs.isOpen)
    assertTrue(fs2.isOpen)

    // Closing fs2 drops its client reference to zero, but the wrapper is still cached,
    // so rawFs must remain open until fs2 is removed from the cache as well.
    fs2.close()
    assertTrue("Raw filesystem rawFs must stay open while fs2 is idle but still cached", rawFs.isOpen)

    fs2.onCacheRemoval()
    assertFalse("Raw filesystem rawFs must close after the last wrapper is evicted", rawFs.isOpen)

    fileSystemProvider.close()
  }

  /**
   * Regression coverage for a wrapper that reopens its raw ZipFileSystem while another
   * wrapper for the same JAR still points to the old, externally closed raw filesystem.
   *
   * The cache must release the reopened raw filesystem instance, not only decrement a
   * URI-level counter attached to whichever stale delegate happens to be closed last.
   */
  @Test
  fun `reopened delegate is closed when its wrapper is evicted before stale wrapper`() {
    val fileSystemProvider = CachingJarFileSystemProvider(retentionTimeInSeconds = Long.MAX_VALUE)

    val fs1 = fileSystemProvider.getFileSystem(jarPath) as FsHandleFileSystem
    val rawFs1 = fs1.initialDelegateFileSystem

    // Keep fs1 active but remove it from the cache, then create fs2. Both wrappers now
    // share rawFs1, while fs1 will be closed independently from the cached fs2.
    fileSystemProvider.evictCachedHandle(jarPath)
    val fs2 = fileSystemProvider.getFileSystem(jarPath) as FsHandleFileSystem
    assertNotSame(fs1, fs2)
    assertSame(rawFs1, fs2.initialDelegateFileSystem)

    // Simulate rawFs1 being closed externally. Using fs1 forces it to reopen and swap
    // its delegate to rawFs2, while fs2 still points to the stale rawFs1 instance.
    rawFs1.close()
    val rawFs2 = fs1.delegateFileSystem
    assertNotSame(rawFs1, rawFs2)
    assertTrue(rawFs2.isOpen)

    // Close the reopened wrapper first. This used to decrement only the URI-level count,
    // leaving rawFs2 open until a stale wrapper released rawFs1.
    fs1.close()
    assertFalse("Reopened raw filesystem rawFs2 must close with fs1 eviction", rawFs2.isOpen)

    fs2.close()
    fs2.onCacheRemoval()

    fileSystemProvider.close()
  }

  @Suppress("UNCHECKED_CAST")
  private fun CachingJarFileSystemProvider.evictCachedHandle(jarPath: Path) {
    val fsCacheField = CachingJarFileSystemProvider::class.java.getDeclaredField("fsCache")
    fsCacheField.isAccessible = true
    val fsCache = fsCacheField.get(this) as Cache<String, FsHandleFileSystem>
    fsCache.invalidate(jarPath.toJarFileUri().toString())
    fsCache.cleanUp()
  }

  // Regression: MP-7468. Targets the TOCTOU window in `unwrapped` — the FS is open at the
  // isClosed() check but closes (from another thread / Caffeine eviction) before the NIO
  // call lands. The stub simulates exactly that race by throwing ClosedFileSystemException
  // on the first invocation without actually closing the FS, so only the catch-and-retry
  // can recover.
  @Test
  fun `read operations retry once on transient ClosedFileSystemException`() {
    val fileSystemProvider = CachingJarFileSystemProvider(retentionTimeInSeconds = Long.MAX_VALUE)
    val fs = fileSystemProvider.getFileSystem(jarPath) as FsHandleFileSystem
    val helloPath = fs.getPath("hello.txt")

    val realDelegateProvider = fs.initialDelegateFileSystem.provider()
    val flaky = ThrowsOnceClosedFsProvider(realDelegateProvider)
    val handler = FsHandlerFileSystemProvider(flaky, NoopJarFileSystemProvider)

    val attrs = handler.readAttributes(helloPath, BasicFileAttributes::class.java)
    assertTrue(attrs.isRegularFile)
    assertEquals(2, flaky.readAttributesCalls.get())

    val content = handler.newByteChannel(helloPath, emptySet()).use { channel ->
      val buf = ByteBuffer.allocate(attrs.size().toInt())
      while (buf.hasRemaining() && channel.read(buf) >= 0) { /* read until EOF */ }
      String(buf.array(), 0, buf.position(), Charsets.UTF_8)
    }
    assertEquals("Hello World", content)
    assertEquals(2, flaky.newByteChannelCalls.get())
  }
}

private object NoopJarFileSystemProvider : JarFileSystemProvider {
  override fun getFileSystem(jarPath: Path): FileSystem = throw UnsupportedOperationException()
}

// FileSystemProvider stub that throws ClosedFileSystemException on the first call to
// newByteChannel / readAttributes(Class) and then delegates. Other methods are not exercised
// by the tests and intentionally throw.
private class ThrowsOnceClosedFsProvider(private val delegate: FileSystemProvider) : FileSystemProvider() {
  val newByteChannelCalls = AtomicInteger(0)
  val readAttributesCalls = AtomicInteger(0)

  override fun getScheme(): String = delegate.scheme

  override fun newByteChannel(path: Path, options: Set<OpenOption>, vararg attrs: FileAttribute<*>): SeekableByteChannel {
    if (newByteChannelCalls.getAndIncrement() == 0) throw ClosedFileSystemException()
    return delegate.newByteChannel(path, options, *attrs)
  }

  override fun <A : BasicFileAttributes> readAttributes(path: Path, type: Class<A>, vararg options: LinkOption): A {
    if (readAttributesCalls.getAndIncrement() == 0) throw ClosedFileSystemException()
    return delegate.readAttributes(path, type, *options)
  }

  override fun newFileSystem(uri: URI, env: Map<String, *>): FileSystem = throw UnsupportedOperationException()
  override fun getFileSystem(uri: URI): FileSystem = throw UnsupportedOperationException()
  override fun getPath(uri: URI): Path = throw UnsupportedOperationException()
  override fun newDirectoryStream(dir: Path, filter: DirectoryStream.Filter<in Path>): DirectoryStream<Path> = throw UnsupportedOperationException()
  override fun createDirectory(dir: Path, vararg attrs: FileAttribute<*>?) = throw UnsupportedOperationException()
  override fun delete(path: Path) = throw UnsupportedOperationException()
  override fun copy(source: Path, target: Path, vararg options: CopyOption) = throw UnsupportedOperationException()
  override fun move(source: Path, target: Path, vararg options: CopyOption) = throw UnsupportedOperationException()
  override fun isSameFile(path: Path, path2: Path): Boolean = throw UnsupportedOperationException()
  override fun isHidden(path: Path): Boolean = throw UnsupportedOperationException()
  override fun getFileStore(path: Path): FileStore = throw UnsupportedOperationException()
  override fun checkAccess(path: Path, vararg modes: AccessMode) = throw UnsupportedOperationException()
  override fun <V : FileAttributeView?> getFileAttributeView(path: Path, type: Class<V>, vararg options: LinkOption): V = throw UnsupportedOperationException()
  override fun readAttributes(path: Path, attributes: String, vararg options: LinkOption): Map<String, Any> = throw UnsupportedOperationException()
  override fun setAttribute(path: Path, attribute: String, value: Any, vararg options: LinkOption) = throw UnsupportedOperationException()
}
