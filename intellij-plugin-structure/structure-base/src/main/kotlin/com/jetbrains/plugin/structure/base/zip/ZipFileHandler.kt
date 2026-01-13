/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.base.zip

import java.io.File
import java.io.IOException
import java.lang.ref.Cleaner
import java.lang.ref.Reference
import java.lang.ref.SoftReference
import java.lang.ref.WeakReference
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipFile

class ZipFileHandler(private val zipFile: File) : ZipHandler<ZipResource.ZipFileResource> {
  constructor(zipPath: Path) : this(zipPath.toFile())

  @Throws(ZipArchiveException::class)
  override fun <T> iterate(handler: (ZipEntry, ZipResource.ZipFileResource) -> T?): List<T> {
    val results = mutableListOf<T>()
    withZip { zip ->
      val entries = zip.entries()
      val zipResource = ZipResource.ZipFileResource(zip)
      while (entries.hasMoreElements()) {
        val entry: ZipEntry? = entries.nextElement()
        entry?.let { handler(entry, zipResource) }
          ?.let { results += it }
      }
    }
    return results
  }

  @Throws(ZipArchiveException::class)
  override fun <T> handleEntry(entryName: CharSequence, handler: (ZipEntry, ZipResource.ZipFileResource) -> T?): T? {
    return withZip { zip ->
      val zipResource = ZipResource.ZipFileResource(zip)
      val entry: ZipEntry? = zip.getEntry(entryName.toString())
      entry?.let { handler(entry, zipResource) }
    }
  }

  override fun containsEntry(entryName: CharSequence): Boolean = withZip { zip ->
    zip.getEntry(entryName.toString()) != null
  }

  private var zipFileHolderRef: Reference<ZipFileHolder>? = null

  @Throws(ZipException::class, IOException::class)
  private fun getActiveHolder(): Pair<ZipFileHolder, Reference<ZipFileHolder>> {
    val (holder: ZipFileHolder?, ref: Reference<ZipFileHolder>?) = synchronized(this) {
      val ref = zipFileHolderRef
      if (ref == null) null to null
      else ref.get() to ref
    }
    if (holder != null) {
      return holder to ref!!
    }
    synchronized(this) {
      var ref = zipFileHolderRef
      var holder = ref?.get()
      if (holder != null) {
        return holder to ref!!
      }
      holder = ZipFileHolder(ZipFile(zipFile))
      ref = createReference(holder)
      zipFileHolderRef = ref
      return holder to ref
    }
  }

  companion object {
    private val refType = System.getProperty("intellij.structure.zip.handler.references", "weak")
    private fun createReference(holder: ZipFileHolder): Reference<ZipFileHolder> {
      return when (refType) {
        "weak" -> WeakReference(holder)
        "soft" -> SoftReference(holder)
        else -> WeakReference(holder)
      }
    }
  }

  private inline fun <R> withZip(block: (ZipFile) -> R): R {
    return try {
      val (holder, ref) = getActiveHolder()
      val result = block.invoke(holder.zipFile)
      // Tricks to keep both ZipFileHolder and reference alive, so it won't be garbage collected
      if (ref !== zipFileHolderRef) {
        throw IllegalStateException("Reference to ZipFileHolder was changed mid-execution")
      }
      val holder2 = zipFileHolderRef?.get()
      if (holder2 !== holder) {
        throw IllegalStateException("ZipFileHolder was changed mid-execution")
      }
      result
    } catch (e: ZipException) {
      throw MalformedZipArchiveException(zipFile, e)
    } catch (e: IOException) {
      throw ZipArchiveIOException(zipFile, e)
    }
  }
}

private class ZipFileHolder(zipFile: ZipFile) : AutoCloseable {
  private val state: State = State(zipFile)
  private val cleanable: Cleaner.Cleanable = CLEANER.register(this, state)

  companion object {
    private val CLEANER = Cleaner.create()
  }

  class State(val zipFile: ZipFile) : Runnable {
    override fun run() {
      zipFile.close()
    }
  }

  override fun close() {
    cleanable.clean()
  }

  val zipFile get() = state.zipFile
}