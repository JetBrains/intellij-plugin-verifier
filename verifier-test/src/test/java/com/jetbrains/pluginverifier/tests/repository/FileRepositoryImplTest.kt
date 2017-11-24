package com.jetbrains.pluginverifier.tests.repository

import com.jetbrains.pluginverifier.misc.exists
import com.jetbrains.pluginverifier.misc.nameWithoutExtension
import com.jetbrains.pluginverifier.misc.readText
import com.jetbrains.pluginverifier.misc.writeText
import com.jetbrains.pluginverifier.repository.cleanup.*
import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount.Companion.ONE_BYTE
import com.jetbrains.pluginverifier.repository.files.*
import org.junit.Assert
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Path
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference


class FileRepositoryImplTest {

  @JvmField
  @Rule
  var tempFolder: TemporaryFolder = TemporaryFolder()

  private fun TemporaryFolder.newFolderPath(): Path = newFolder().toPath()

  @Test
  fun `basic operations`() {
    val folder = tempFolder.newFolderPath()
    val fileRepository: FileRepository<Int> = FileRepositoryImpl.createFromExistingFiles(
        folder,
        SimulationDownloader(),
        IntFileNameMapper(),
        IdleSweepPolicy
    )

    val get0 = fileRepository.get(0) as FileRepositoryResult.Found
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

    val fileRepository = FileRepositoryImpl.createFromExistingFiles(
        folder,
        SimulationDownloader(),
        IntFileNameMapper(),
        IdleSweepPolicy,
        keyProvider = { it.nameWithoutExtension.toIntOrNull() }
    )

    val get0 = fileRepository.get(0) as FileRepositoryResult.Found
    assertEquals("0", get0.lockedFile.file.readText())
    get0.lockedFile.release()

    val get1 = fileRepository.get(1) as FileRepositoryResult.Found
    assertEquals("1", get1.lockedFile.file.readText())
    get1.lockedFile.release()
  }

  @Test
  fun `only one of the concurrent threads downloads the file`() {
    val downloader = OnlyOneDownloadAtTimeDownloader()

    val fileRepository = FileRepositoryImpl.createFromExistingFiles(
        tempFolder.newFolderPath(),
        downloader,
        IntFileNameMapper(),
        IdleSweepPolicy
    )

    val numberOfThreads = 10
    val executorService = Executors.newFixedThreadPool(numberOfThreads)
    try {
      executorService.invokeAll((1..numberOfThreads).map {
        Callable {
          //Add random delay before taking the 0-th element
          Thread.sleep(Math.abs(Random().nextLong()) % 1000)
          fileRepository.get(0)
        }
      })
    } finally {
      executorService.shutdownNow()
    }

    assertThat(downloader.errors, org.hamcrest.Matchers.empty())
  }

  private fun <K> FileRepository<K>.getFound(k: K) = get(k) as FileRepositoryResult.Found

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

    val fileRepository = FileRepositoryImpl.createFromExistingFiles(
        tempFolder.newFolderPath(),
        SimulationDownloader(),
        IntFileNameMapper(),
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
      assertTrue(file.toFile().fileSize == ONE_BYTE)
    }
    assertTrue(repositoryDir.fileSize == ONE_BYTE * 10)

    //create the file repository with maximum cache size of 5 bytes,
    // low space threshold 2 bytes and after-cleanup free space 3 bytes
    FileRepositoryImpl.createFromExistingFiles(
        repositoryDir,
        SimulationDownloader(),
        IntFileNameMapper(),
        LruFileSizeSweepPolicy(DiskSpaceSetting(ONE_BYTE * 5, ONE_BYTE * 2, ONE_BYTE * 3)),
        keyProvider = { it.nameWithoutExtension.toIntOrNull() }
    )

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
    val downloadStarted = AtomicBoolean()
    val removeCalled = AtomicBoolean()

    val downloader = SimulationDownloader {
      downloadStarted.set(true)

      //simulating the downloading until the 'remove' method is called
      while (!removeCalled.get()) {
      }
    }

    val fileRepository = FileRepositoryImpl.createFromExistingFiles(
        tempFolder.newFolderPath(),
        downloader,
        IntFileNameMapper(),
        IdleSweepPolicy
    )

    val fileLock = AtomicReference<FileLock>()
    val downloadThread = Thread {
      val lockedFile = (fileRepository.get(0) as FileRepositoryResult.Found).lockedFile
      fileLock.set(lockedFile)
    }

    val removeThread = Thread {
      //waiting until the downloading is started
      while (!downloadStarted.get()) {
      }

      //call the remove in the time of downloading
      fileRepository.remove(0)

      removeCalled.set(true)
    }

    downloadThread.start()
    removeThread.start()

    downloadThread.join()
    removeThread.join()

    Assert.assertTrue(fileLock.get().file.exists())
    fileLock.get().release()
    Assert.assertFalse(fileLock.get().file.exists())
  }
}