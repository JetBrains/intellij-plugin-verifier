package com.jetbrains.pluginverifier.reporting.common

import com.jetbrains.pluginverifier.misc.create
import com.jetbrains.pluginverifier.reporting.Reporter
import java.io.File

open class FileReporter<in T>(file: File, private val lineProvider: (T) -> String = { it.toString() }) : Reporter<T> {

  private val writer by lazy { file.create().bufferedWriter() }

  override fun report(t: T) {
    writer.appendln(lineProvider(t))
  }

  override fun close() {
    writer.close()
  }
}