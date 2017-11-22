package com.jetbrains.pluginverifier.tests.repository

import com.jetbrains.pluginverifier.repository.cleanup.*
import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount.Companion.ONE_BYTE
import com.jetbrains.pluginverifier.repository.files.AvailableFile
import com.jetbrains.pluginverifier.repository.files.FileRepository
import com.jetbrains.pluginverifier.repository.files.FileRepositoryImpl
import com.jetbrains.pluginverifier.repository.files.FileRepositoryResult
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors


class FileRepositoryImplTest {

  @JvmField
  @Rule
  var tempFolder: TemporaryFolder = TemporaryFolder()

  @Test
  fun `basic operations`() {
    val folder = tempFolder.newFolder()
    val fileRepository: FileRepository<Int> = FileRepositoryImpl(
        folder,
        MockDownloader(),
        IntFileKeyMapper(),
        MockSweepPolicy()
    )

    val get0 = fileRepository.get(0) as FileRepositoryResult.Found
    val get0Locked = get0.lockedFile
    val get0File = get0Locked.file
    val expectedFile = File(folder, "0")
    assertEquals(expectedFile, get0File)
    assertEquals("0", get0File.readText())

    assertFalse(fileRepository.remove(0))
    get0Locked.release()
    assertFalse(fileRepository.has(0))
  }

  @Test
  fun `file repository created with existing files`() {
    val folder = tempFolder.newFolder()
    folder.resolve("0").writeText("0")
    folder.resolve("1").writeText("1")

    val fileRepository = FileRepositoryImpl(
        folder,
        MockDownloader(),
        IntFileKeyMapper(),
        MockSweepPolicy()
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

    val fileRepository = FileRepositoryImpl(
        tempFolder.newFolder(),
        downloader,
        IntFileKeyMapper(),
        MockSweepPolicy()
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

    val fileRepository = FileRepositoryImpl(
        tempFolder.newFolder(),
        MockDownloader(),
        IntFileKeyMapper(),
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
    val repositoryDir = tempFolder.newFolder()

    //create 10 files of size 1 byte
    for (i in 1..10) {
      val file = repositoryDir.resolve(i.toString())
      file.writeBytes(byteArrayOf(0))
      assertTrue(file.fileSize == ONE_BYTE)
    }
    assertTrue(repositoryDir.fileSize == ONE_BYTE * 10)

    //create the file repository with maximum cache size of 5 bytes,
    // low space threshold 2 bytes and after-cleanup free space 3 bytes
    FileRepositoryImpl(
        repositoryDir,
        MockDownloader(),
        IntFileKeyMapper(),
        LruFileSizeSweepPolicy(DiskSpaceSetting(ONE_BYTE * 5, ONE_BYTE * 2, ONE_BYTE * 3))
    )

    //cleanup procedure on repository startup must make its size 2 bytes (i.e. remove 8 files)
    val repoSize = repositoryDir.fileSize
    assertEquals(ONE_BYTE * 2, repoSize)
  }

}