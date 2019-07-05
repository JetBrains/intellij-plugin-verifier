package com.jetbrains.plugin.structure.testUtils.contentBuilder

import java.io.File
import java.lang.IllegalArgumentException

interface ContentBuilder {
  fun file(name: String)
  fun file(name: String, text: String)
  fun file(name: String, textProvider: () -> String)
  fun file(name: String, content: ByteArray)
  fun file(name: String, localFile: File)
  fun dir(name: String, localDirectory: File)
  fun dir(name: String, content: ContentBuilder.() -> Unit)
  fun zip(name: String, content: ContentBuilder.() -> Unit)
}

fun buildDirectory(directory: File, content: ContentBuilder.() -> Unit): File {
  val spec = buildDirectoryContent(content)
  spec.generate(directory)
  return directory
}

fun buildZipFile(zipFile: File, content: ContentBuilder.() -> Unit): File {
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

  override fun file(name: String, localFile: File) {
    file(name, localFile.readBytes())
  }

  override fun dir(name: String, content: ContentBuilder.() -> Unit) {
    val directorySpec = buildDirectoryContent(content)
    addChild(name, directorySpec)
  }

  override fun dir(name: String, localDirectory: File) {
    check(localDirectory.isDirectory) { "Not a directory: $localDirectory" }
    dir(name) {
      for (child in localDirectory.listFiles().orEmpty()) {
        when {
          child.isFile -> file(child.name, child)
          child.isDirectory -> dir(child.name, child)
          else -> throw IllegalArgumentException("Unknown file type: ${child.absoluteFile}")
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
