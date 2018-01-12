package com.jetbrains.plugin.structure.domain

import java.io.File
import java.util.jar.Attributes
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import java.util.jar.Manifest

data class JarFileEntry(val file: File, val name: String)

object JarFileUtils {

  /**
   * Creates a jar file containing a set of [JarFileEntry].
   */
  fun createJarFile(entries: List<JarFileEntry>, output: File) {
    val manifest = Manifest()
    manifest.mainAttributes[Attributes.Name.MANIFEST_VERSION] = "1.0"
    JarOutputStream(output.outputStream().buffered(), manifest).use { target ->
      for (entry in entries) {
        addFile(entry, target)
      }
    }
  }

  private fun addFile(source: JarFileEntry, target: JarOutputStream) {
    if (source.file.isDirectory) {
      var name = source.name
      if (!name.isEmpty()) {
        if (!name.endsWith("/")) {
          name += "/"
        }
        val entry = JarEntry(name)
        entry.time = source.file.lastModified()
        target.putNextEntry(entry)
        target.closeEntry()
      }
      for (nestedFile in source.file.listFiles()) {
        addFile(JarFileEntry(nestedFile, name + "/" + nestedFile.name), target)
      }
    } else {
      val entry = JarEntry(source.name.replace("\\", "/"))
      entry.time = source.file.lastModified()
      target.putNextEntry(entry)

      source.file.inputStream().buffered().use { `in` ->
        `in`.copyTo(target)
        target.closeEntry()
      }
    }
  }
}
