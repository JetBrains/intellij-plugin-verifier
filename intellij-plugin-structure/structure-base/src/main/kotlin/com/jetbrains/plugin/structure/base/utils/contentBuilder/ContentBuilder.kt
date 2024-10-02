/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.base.utils.contentBuilder

import com.jetbrains.plugin.structure.base.utils.createDir
import com.jetbrains.plugin.structure.base.utils.isDirectory
import com.jetbrains.plugin.structure.base.utils.isFile
import com.jetbrains.plugin.structure.base.utils.listFiles
import com.jetbrains.plugin.structure.base.utils.readBytes
import com.jetbrains.plugin.structure.base.utils.simpleName
import java.nio.file.Path

interface ContentBuilder {
  fun file(name: String)
  fun file(name: String, text: String)
  fun file(name: String, textProvider: () -> String)
  fun file(name: String, content: ByteArray)
  fun file(name: String, localFile: Path)
  fun dir(name: String, localDirectory: Path)
  fun dir(name: String, content: ContentBuilder.() -> Unit)
  fun dirs(name: String, content: ContentBuilder.() -> Unit)
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

class ContentBuilderImpl(private val result: ChildrenOwnerSpec) : ContentBuilder {
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

  override fun dirs(name: String, content: ContentBuilder.() -> Unit) {
    val pathElements = name.split("/")
    when (pathElements.size) {
      0 -> throw IllegalArgumentException("Cannot have empty name")
      1 -> dir(pathElements.first(), content)
      else -> {
        val firstElement = pathElements.first()
        // /home/jetbrains/data/
        // first ->home
        val w = pathElements.drop(1).windowed(2).reversed()
        val tree = w.mapIndexed { i, (parent, child) ->
          val childSpec = if (i == 0) {
            SingleChildSpec(child, buildDirectoryContent(content))
          } else {
            SingleChildSpec(child)
          }
          val parentSpec = SingleChildSpec(parent, childSpec)
          parentSpec
        }

        val tree2 = ArrayList(tree).apply { add(null) }
        for (index in 0 until tree2.size - 1) {
          val current = tree2[index] ?: continue
          val next = tree2[index + 1]
          if (next != null) {
            val n = next.copy(child = current)
            tree2[index + 1] = n
          } else {
            if (index + 1 == tree2.size - 1) {
              /*
              current.replaceChild(content)?.let {
                tree2[index] = it
              }

               */
            }
          }
        }
        println(tree2)
        addChild(firstElement, tree2.dropLast(1).last())
      }
    }
  }

  private fun SingleChildSpec.deepestChild(): SingleChildSpec? {
    var c = this.child
    var result: SingleChildSpec? = null
    while (true) {
      if (c is SingleChildSpec) {
        result = c
        c = c.child
      } else {
        break
      }
    }
    return result
  }

  private fun SingleChildSpec.replaceChild(content: ContentBuilder.() -> Unit): SingleChildSpec? {
    return when (child) {
      is SingleChildSpec -> {
        val newChild = child.copy(child = buildDirectoryContent(content))
        copy(child = newChild)
      }

      is DirectorySpec -> {
        copy(child = buildDirectoryContent(content))
      }

      else -> {
        null
      }
    }
  }

  private data class SingleChildSpec(val name: String, val child: ContentSpec) : ContentSpec {
    constructor(name: String) : this(name, DirectorySpec())

    override fun generate(target: Path) {
      target.createDir()
      val childFile = target.resolve(name)
      child.generate(childFile)
    }

    override fun toString(): String {
      return "$name/$child"
    }
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
