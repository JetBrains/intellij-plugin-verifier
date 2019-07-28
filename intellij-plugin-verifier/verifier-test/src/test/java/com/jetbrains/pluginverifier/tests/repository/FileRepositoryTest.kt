package com.jetbrains.pluginverifier.tests.repository

import com.jetbrains.plugin.structure.base.utils.*
import com.jetbrains.pluginverifier.repository.cleanup.*
import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount.Companion.ONE_BYTE
import com.jetbrains.pluginverifier.repository.downloader.DownloadProvider
import com.jetbrains.pluginverifier.repository.files.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Path
import java.time.Clock
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs


class FileRepositoryTest {

  @JvmField
  @Rule
  var tempFolder: TemporaryFolder = TemporaryFolder()

  private fun TemporaryFolder.newFolderPath(): Path = newFolder().toPath()

  private fun createDownloadingProvider(downloadDir: Path) = DownloadProvider(downloadDir, SimulationDownloader(), IntFileNameMapper())

  @Test
  fun `basic operations`() {
    val folder = tempFolder.newFolderPath()
    val fileRepository = FileRepositoryBuilder<Int>()
        .clock(Clock.systemUTC())
        .resourceProvider(createDownloadingProvider(folder))
        .build()

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

    val fileRepository = FileRepositoryBuilder<Int>()
        .resourceProvider(createDownloadingProvider(folder))
        .addInitialFilesFrom(folder) { it.nameWithoutExtension.toIntOrNull() }
        .build()

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

    val fileRepository = FileRepositoryBuilder<Int>()
        .resourceProvider(DownloadProvider(tempFolder.newFolderPath(), downloader, IntFileNameMapper()))
        .build()

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

  private fun <K> FileRepository<K>.getFound(k: K) = getFile(k) as FileRepositoryResult.Found

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

    val fileRepository = FileRepositoryBuilder<Int>()
        .sweepPolicy(lruNSweepPolicy)
        .resourceProvider(createDownloadingProvider(tempFolder.newFolderPath()))
        .build()

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
    FileRepositoryBuilder<Int>()
        .sweepPolicy(LruFileSizeSweepPolicy(DiskSpaceSetting(ONE_BYTE * 5, ONE_BYTE * 2, ONE_BYTE * 3)))
        .resourceProvider(createDownloadingProvider(repositoryDir))
        .addInitialFilesFrom(repositoryDir) { it.nameWithoutExtension.toIntOrNull() }
        .build()

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
      @Suppress("ControlFlowWithEmptyBody")
      while (!removeCalled.get()) {
      }
    }

    val fileRepository = FileRepositoryBuilder<Int>()
        .resourceProvider(DownloadProvider(tempFolder.newFolderPath(), downloader, IntFileNameMapper()))
        .build()

    val fileLock = AtomicReference<FileLock>()
    val downloadThread = Thread {
      val lockedFile = (fileRepository.getFound(0)).lockedFile
      fileLock.set(lockedFile)
    }

    val removeThread = Thread {
      //waiting until the downloading is started
      @Suppress("ControlFlowWithEmptyBody")
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

    val fileRepository = FileRepositoryBuilder<Int>()
        .resourceProvider(createDownloadingProvider(tempFolder))
        .addInitialFilesFrom(tempFolder) { it.nameWithoutExtension.toIntOrNull() }
        .build()

    assertEquals(setOf(1), fileRepository.getAllExistingKeys())
    assertEquals(listOf(trueFile), tempFolder.listFiles())
  }

  @Test
  fun `all existing keys`() {
    val tempFolder = tempFolder.newFolderPath()
    val fileRepository = FileRepositoryBuilder<Int>()
        .resourceProvider(createDownloadingProvider(tempFolder))
        .build()
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
}