package org.jetbrains.plugins.verifier.service.ide

import java.io.Closeable
import java.io.File

/**
 * @author Sergey Patrikeev
 */
abstract class IdeFileLock : Closeable {
  abstract fun getIdeFile(): File

  abstract fun release()

  final override fun close() = release()
}