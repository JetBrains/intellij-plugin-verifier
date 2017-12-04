package org.jetbrains.plugins.verifier.service.server.database

import java.io.Closeable

/**
 * Server database allows to persist data between the server start-ups.
 *
 * The database must be [closed] [close] on the server shutdown.
 */
interface ServerDatabase : Closeable {

  fun getProperty(key: String): String?

  fun setProperty(key: String, value: String): String?

  /**
   * Flush allocated database resources and save data.
   */
  override fun close() = Unit
}