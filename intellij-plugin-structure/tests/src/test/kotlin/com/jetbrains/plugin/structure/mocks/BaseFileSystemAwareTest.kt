package com.jetbrains.plugin.structure.mocks

import com.jetbrains.plugin.structure.rules.FileSystemAwareTemporaryFolder
import com.jetbrains.plugin.structure.rules.FileSystemType
import org.junit.Rule
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
abstract class BaseFileSystemAwareTest(fileSystemType: FileSystemType) {

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "file-system={0}")
    fun fsType(): List<Array<FileSystemType>> = listOf(arrayOf(FileSystemType.IN_MEMORY), arrayOf(FileSystemType.DEFAULT))
  }

  @Rule
  @JvmField
  val temporaryFolder = FileSystemAwareTemporaryFolder(fileSystemType)

}