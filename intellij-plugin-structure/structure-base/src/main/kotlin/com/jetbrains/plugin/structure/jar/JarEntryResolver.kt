package com.jetbrains.plugin.structure.jar

import java.util.zip.ZipEntry

interface JarEntryResolver<T> {
  val key: Key<T>

  fun resolve(path: PathInJar, zipEntry: ZipEntry): T?

  data class Key<T>(val name: String, val type: Class<T>)
}
