/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.xinclude

import com.intellij.platform.pluginSystem.parser.impl.LoadedXIncludeReference
import com.intellij.platform.pluginSystem.parser.impl.XIncludeLoader

/**
 * Transparently records the resource requests made by the platform parser.
 *
 * The delegate remains the sole authority for resolving an include. This wrapper returns the exact
 * [LoadedXIncludeReference] produced by it, so recording cannot change parser behaviour. Occurrences
 * are deliberately not deduplicated: including the same document twice makes the parser consume its
 * declarations twice too.
 */
internal class RecordingXIncludeLoader(
  private val delegate: XIncludeLoader
) : XIncludeLoader {
  private val recordedAttempts = mutableListOf<XIncludeLoadAttempt>()

  val attempts: List<XIncludeLoadAttempt>
    get() = recordedAttempts.toList()

  override fun loadXIncludeReference(path: String): LoadedXIncludeReference? {
    return try {
      val loaded = delegate.loadXIncludeReference(path)
      recordedAttempts += if (loaded == null) {
        XIncludeLoadAttempt.Missing(path)
      } else {
        XIncludeLoadAttempt.Loaded(path, loaded.inputStream, loaded.diagnosticReferenceLocation)
      }
      loaded
    } catch (e: Exception) {
      recordedAttempts += XIncludeLoadAttempt.Failed(path, e.javaClass.name, e.message)
      throw e
    }
  }
}

internal sealed class XIncludeLoadAttempt {
  abstract val path: String

  data class Loaded(
    override val path: String,
    val bytes: ByteArray,
    val diagnosticLocation: String?
  ) : XIncludeLoadAttempt()

  data class Missing(override val path: String) : XIncludeLoadAttempt()

  data class Failed(
    override val path: String,
    val exceptionClass: String,
    val message: String?
  ) : XIncludeLoadAttempt()
}
