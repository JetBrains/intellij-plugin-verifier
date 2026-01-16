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

  /**
   * Either opens new `ZipFile` or reuses existing one stored under [reference][zipFileHolderRef] (WeakReference or SoftReference).
   * `ZipFileHolder` used to perform `ZipFile` closing once it's no longer reachable from GC roots.
   *
   * In this method [reference][zipFileHolderRef] is accessed with synchronization to ensure we won't create more than one ZipFileHolder.
   *
   * Three states are possible:
   * 1. Reference is null, no `ZipFileHolder` is created yet
   * 2. Reference is not null, but `ZipFileHolder` is null, which means that `ZipFileHolder` was garbage collected
   * 3. Reference and `ZipFileHolder` are not null, `ZipFileHolder` is still valid
   *
   * In the case of the first two, we create a new `ZipFile`, `ZipFileHolder` and store it under [reference][zipFileHolderRef], so it becomes state three.
   */
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

      //region Tricks to keep both ZipFileHolder and reference alive, so it won't be garbage collected
      // It's OK to access zipFileHolderRef without synchronization here.
      // Since `ref` and `holder` are accessible, `zipFileHolderRef` cannot change
      if (ref !== zipFileHolderRef) {
        throw IllegalStateException("Reference to ZipFileHolder was changed mid-execution")
      }
      val holder2 = zipFileHolderRef?.get()
      if (holder2 !== holder) {
        throw IllegalStateException("ZipFileHolder was changed mid-execution")
      }
      //endregion

      result
    } catch (e: ZipException) {
      throw MalformedZipArchiveException(zipFile, e)
    } catch (e: IOException) {
      throw ZipArchiveIOException(zipFile, e)
    }
  }
}

/**
 * Holder for ZipFile. Although it implements AutoCloseable, we don't use that.
 * Once there are no more references to ZipFileHolder, ZipFile will be closed by Cleaner
 */
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