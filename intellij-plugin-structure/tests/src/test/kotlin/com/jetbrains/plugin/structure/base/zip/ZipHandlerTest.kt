/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.base.zip

import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.nio.file.Path

@RunWith(Parameterized::class)
class ZipHandlerTest<T : ZipResource>(private val type: ZipHandlerType) {
  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "zip-handler={0}")
    fun zipHandler() = listOf(arrayOf(ZipHandlerType.FILE), arrayOf(ZipHandlerType.STREAM))
  }

  enum class ZipHandlerType {
    FILE,
    STREAM
  }
  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  @Test
  fun `archive files are correctly discovered or correctly not found`() {
    val jarPath = buildZipFile(createJar()) {
      file("intellij.example.xml", "<idea-plugin />")
      dir("META-INF") {
        file("plugin.properties", "name=My Class")
        dir("services") {
          file("kotlinx.coroutines.internal.MainDispatcherFactory") {
            "com.intellij.openapi.application.impl.EdtCoroutineDispatcherFactory"
          }
        }
        file("plugin.xml", "<idea-plugin />")
      }
    }
    val zipHandler = newZipHandler(jarPath)
    assertTrue(zipHandler.containsEntry("META-INF/plugin.xml"))
    assertTrue(zipHandler.containsEntry("intellij.example.xml"))
    assertFalse(zipHandler.containsEntry("nonexistent.txt"))
  }

  private fun newZipHandler(zipPath: Path) = when (type) {
    ZipHandlerType.FILE -> ZipFileHandler(zipPath)
    ZipHandlerType.STREAM -> ZipInputStreamHandler(zipPath)
  }

  fun createJar(): Path {
    return temporaryFolder.newFile("plugin.jar").toPath()
  }
}