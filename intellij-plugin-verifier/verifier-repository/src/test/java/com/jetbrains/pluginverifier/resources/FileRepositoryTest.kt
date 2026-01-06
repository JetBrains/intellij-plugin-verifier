/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.resources

import com.jetbrains.plugin.structure.base.utils.*
import com.jetbrains.pluginverifier.repository.cleanup.*
import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount.Companion.ONE_BYTE
import com.jetbrains.pluginverifier.repository.downloader.DownloadProvider
import com.jetbrains.pluginverifier.repository.downloader.DownloadResult
import com.jetbrains.pluginverifier.repository.downloader.Downloader
import com.jetbrains.pluginverifier.repository.files.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Path
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs


class FileRepositoryTest {

  @JvmField
  @Rule
  var tempFolder: TemporaryFolder = TemporaryFolder()

  private fun TemporaryFolder.newFolderPath(): Path = newFolder().toPath()

  private fun createDownloadingProvider(downloadDir: Path) = DownloadProvider(downloadDir, SimulationDownloader()) { it.toString() }

  @Test
  fun `basic operations`() {
    val folder = tempFolder.newFolderPath()
    val fileRepository = FileRepository(
      createDownloadingProvider(folder),
      IdleSweepPolicy()
    )

    val get0 = fileRepository.getFile(0) as FileRepositoryResult.Found
    val get0Locked = get0.lockedFile
    val get0File = get0Locked.file
    val expectedFile = folder.resolve("0")
    assertEquals(expectedFile, get0File)
    assertEquals("0", get0File.readText())

    assertFalse(fileRepository.remove(0))
    get0Locked.release()
    assertFalse(fileRepository.has(0))
  }

  @Test
  fun `file repository created with existing files`() {
    val folder = tempFolder.newFolderPath()
    folder.resolve("0").writeText("0")
    folder.resolve("1").writeText("1")

    val fileRepository = FileRepository(
      createDownloadingProvider(folder),
      IdleSweepPolicy()
    )
      .addInitialFilesFrom(folder) { it.nameWithoutExtension.toIntOrNull() }

    val get0 = fileRepository.getFound(0)
    assertEquals("0", get0.lockedFile.file.readText())
    get0.lockedFile.release()

    val get1 = fileRepository.getFound(1)
    assertEquals("1", get1.lockedFile.file.readText())
    get1.lockedFile.release()
  }

  @Test
  fun `only one of the concurrent threads downloads the file`() {
    val downloader = OnlyOneDownloadAtTimeDownloader()

    val fileRepository = FileRepository(
      DownloadProvider(tempFolder.newFolderPath(), downloader) { it.toString() },
      IdleSweepPolicy()
    )

    val numberOfThreads = 10
    val executorService = Executors.newFixedThreadPool(numberOfThreads)
    try {
      executorService.invokeAll((1..numberOfThreads).map {
        Callable {
          //Add random delay before taking the 0-th element
          Thread.sleep(abs(Random().nextLong()) % 1000)
          fileRepository.getFile(0)
        }
      })
    } finally {
      executorService.shutdownAndAwaitTermination(1, TimeUnit.MINUTES)
    }

    assertTrue(downloader.errors.isEmpty())
  }

  private fun <K : Any> FileRepository<K>.getFound(k: K) = getFile(k) as FileRepositoryResult.Found

  @Test
  fun `file sweeper removes files using LRU order when the file is released`() {
    val n = 2
    val lruNSweepPolicy = object : SweepPolicy<Int> {
      override fun isNecessary(totalSpaceUsed: SpaceAmount): Boolean = true

      override fun selectFilesForDeletion(sweepInfo: SweepInfo<Int>): List<AvailableFile<Int>> =
        sweepInfo.availableFiles
          .sortedBy { it.usageStatistic.lastAccessTime }
          .dropLast(n)
    }

    val fileRepository = FileRepository(
      createDownloadingProvider(tempFolder.newFolderPath()),
      lruNSweepPolicy
    )

    val found0 = fileRepository.getFound(0)
    val found1 = fileRepository.getFound(1)
    val found2 = fileRepository.getFound(2)

    assertTrue(fileRepository.has(0))
    assertTrue(fileRepository.has(1))
    assertTrue(fileRepository.has(2))

    found0.lockedFile.release()
    found1.lockedFile.release()
    found2.lockedFile.release()
    assertFalse(fileRepository.has(0))
    assertTrue(fileRepository.has(1))
    assertTrue(fileRepository.has(2))
  }

  @Test
  fun `delete the heaviest files on repository creation if necessary`() {
    val repositoryDir = tempFolder.newFolderPath()

    //create 10 files of size 1 byte
    for (i in 1..10) {
      val file = repositoryDir.resolve(i.toString())
      file.toFile().writeBytes(byteArrayOf(0))
      assertTrue(file.fileSize == ONE_BYTE)
    }
    assertTrue(repositoryDir.fileSize == ONE_BYTE * 10)

    //create the file repository with maximum cache size of 5 bytes,
    // low space threshold 2 bytes and after-cleanup free space 3 bytes
    FileRepository(
      createDownloadingProvider(repositoryDir),
      LruFileSizeSweepPolicy(DiskSpaceSetting(ONE_BYTE * 5, ONE_BYTE * 2, ONE_BYTE * 3))
    ).addInitialFilesFrom(repositoryDir) { it.nameWithoutExtension.toIntOrNull() }

    //cleanup procedure on repository startup must make its size 2 bytes (i.e. remove 8 files)
    val repoSize = repositoryDir.fileSize
    assertEquals(ONE_BYTE * 2, repoSize)
  }

  /**
   * Test the following case:
   *
   * A thread calls *remove* for a file while another thread is downloading the file.
   *
   * The downloading thread must successfully obtain the file lock,
   * and once it releases the lock, the file must be removed.
   */
  @Test
  fun `delete the file which is being downloaded`() {
    val downloadStarted = CountDownLatch(1)
    val removeCalled = CountDownLatch(1)

    val downloader = SimulationDownloader {
      downloadStarted.countDown()

      //simulating the downloading until the 'remove' method is called
      removeCalled.await()
    }

    val fileRepository = FileRepository(
      DownloadProvider(tempFolder.newFolderPath(), downloader) { it.toString() },
      IdleSweepPolicy()
    )

    val fileLock = AtomicReference<FileLock>()
    val downloadThread = Thread {
      val lockedFile = (fileRepository.getFound(0)).lockedFile
      fileLock.set(lockedFile)
    }

    val removeThread = Thread {
      //waiting until the downloading is started
      downloadStarted.await()

      //call the remove in the time of downloading
      fileRepository.remove(0)

      removeCalled.countDown()
    }

    downloadThread.start()
    removeThread.start()

    downloadThread.join()
    removeThread.join()

    assertTrue(fileLock.get().file.exists())
    fileLock.get().release()
    assertFalse(fileLock.get().file.exists())
  }

  @Test
  fun `delete extra files on the repository directory creation`() {
    val tempFolder = tempFolder.newFolderPath()
    //These two files are extra files that must be removed on the repository creation
    tempFolder.resolve("one.txt").writeText("one")
    tempFolder.resolve("downloads").createDir()

    //This is the only legitimate file
    val trueFile = tempFolder.resolve("1")
    trueFile.writeText("1")

    val fileRepository = FileRepository(
      createDownloadingProvider(tempFolder),
      IdleSweepPolicy()
    ).addInitialFilesFrom(tempFolder) { it.nameWithoutExtension.toIntOrNull() }

    assertEquals(setOf(1), fileRepository.getAllExistingKeys())
    assertEquals(listOf(trueFile), tempFolder.listFiles())
  }

  @Test
  fun `all existing keys`() {
    val tempFolder = tempFolder.newFolderPath()
    val fileRepository = FileRepository(
      createDownloadingProvider(tempFolder),
      IdleSweepPolicy()
    )
    with(fileRepository) {
      val one = getFile(1) as FileRepositoryResult.Found
      val two = getFile(2) as FileRepositoryResult.Found
      assertEquals(setOf(1, 2), getAllExistingKeys())
      one.lockedFile.release()
      two.lockedFile.release()
      //releasing of keys should not lead to eviction.
      assertEquals(setOf(1, 2), getAllExistingKeys())
      remove(1)
      assertEquals(setOf(2), getAllExistingKeys())
      remove(2)
      assertEquals(emptySet<Int>(), getAllExistingKeys())
    }
  }

  @Test
  fun `download provider allows to download files with the same name`() {
    val downloadDir = tempFolder.newFolder().toPath()
    downloadDir.resolve("a.txt").writeText("0")

    var nextText = 1
    val downloader = object : Downloader<Unit> {
      override fun download(key: Unit, tempDirectory: Path): DownloadResult {
        val result = tempDirectory.resolve("a.txt")
        result.writeText((nextText++).toString())
        return DownloadResult.Downloaded(result, "txt", false)
      }
    }
    val downloadProvider = DownloadProvider(downloadDir, downloader) { "a" }

    downloadProvider.provide(Unit)
    downloadProvider.provide(Unit)
    downloadProvider.provide(Unit)

    assertEquals("0", downloadDir.resolve("a.txt").readText())
    assertEquals("1", downloadDir.resolve("a (1).txt").readText())
    assertEquals("2", downloadDir.resolve("a (2).txt").readText())
    assertEquals("3", downloadDir.resolve("a (3).txt").readText())
  }
}