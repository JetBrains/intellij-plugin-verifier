package com.jetbrains.pluginverifier.tests.repository

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
        MockFileKeyMapper(),
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
        MockFileKeyMapper(),
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
        MockFileKeyMapper(),
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
}