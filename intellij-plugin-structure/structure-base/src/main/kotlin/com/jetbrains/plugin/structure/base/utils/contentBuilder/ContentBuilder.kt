/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.base.utils.contentBuilder

import com.jetbrains.plugin.structure.base.utils.*
import java.io.File
import java.nio.file.Path

interface ContentBuilder {
  fun file(name: String)
  fun file(name: String, text: String)
  fun file(name: String, textProvider: () -> String)
  fun file(name: String, content: ByteArray)
  fun file(name: String, localFile: Path)
  fun dir(name: String, localDirectory: Path)
  fun dir(name: String, content: ContentBuilder.() -> Unit)
  fun zip(name: String, content: ContentBuilder.() -> Unit)
}

fun buildDirectory(directory: Path, content: ContentBuilder.() -> Unit): Path {
  val spec = buildDirectoryContent(content)
  spec.generate(directory)
  return directory
}

fun buildZipFile(zipFile: Path, content: ContentBuilder.() -> Unit): Path {
  val spec = buildZipFileContent(content)
  spec.generate(zipFile)
  return zipFile
}

fun buildDirectoryContent(content: ContentBuilder.() -> Unit): ContentSpec {
  val result = DirectorySpec()
  ContentBuilderImpl(result).content()
  return result
}

fun buildZipFileContent(content: ContentBuilder.() -> Unit): ContentSpec {
  val result = ZipSpec()
  ContentBuilderImpl(result).content()
  return result
}

private class ContentBuilderImpl(private val result: ChildrenOwnerSpec) : ContentBuilder {
  override fun file(name: String) {
    file(name, "")
  }

  override fun file(name: String, textProvider: () -> String) {
    file(name, textProvider())
  }

  override fun file(name: String, text: String) {
    file(name, text.toByteArray())
  }

  override fun file(name: String, content: ByteArray) {
    addChild(name, FileSpec(content))
  }

  override fun file(name: String, localFile: Path) {
    file(name, localFile.readBytes())
  }

  override fun dir(name: String, content: ContentBuilder.() -> Unit) {
    val directorySpec = buildDirectoryContent(content)
    addChild(name, directorySpec)
  }

  override fun dir(name: String, localDirectory: Path) {
    check(localDirectory.isDirectory) { "Not a directory: $localDirectory" }
    dir(name) {
      for (child in localDirectory.listFiles().orEmpty()) {
        when {
          child.isFile -> file(child.simpleName, child)
          child.isDirectory -> dir(child.simpleName, child)
          else -> throw IllegalArgumentException("Unknown file type: ${child.toAbsolutePath()}")
        }
      }
    }
  }

  override fun zip(name: String, content: ContentBuilder.() -> Unit) {
    val zipFileContent = buildZipFileContent(content)
    addChild(name, zipFileContent)
  }

  private fun addChild(name: String, spec: ContentSpec) {
    result.addChild(name, spec)
  }
}
