package com.jetbrains.plugin.structure.base.utils

import org.junit.Assert.fail
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.file.Path

internal fun TemporaryFolder.newTemporaryFile(filePath: String, handleFile: (File) -> Unit = {}): Path {
  val pathComponents = filePath.split("/")
  val dirComponents = pathComponents.dropLast(1).toTypedArray()
  if (dirComponents.isEmpty()) {
    fail("Cannot create temporary file '$filePath'")
  }
  val fileComponent = pathComponents.last()
  val folder: File = newFolder(*dirComponents)
  return File(folder, fileComponent)
    .also { handleFile(it) }
    .toPath()
}