package org.jetbrains.plugins.verifier.service.server.database

import java.io.Closeable

/**
 * Server database allows to persist data between the server start-ups.
 *
 * The database must be [closed] [close] on the server shutdown.
 */
interface ServerDatabase : Closeable {

  fun <T> openOrCreateSet(setName: String, elementType: ValueType<T>): MutableSet<T>

  fun <K, V> openOrCreateMap(mapName: String, keyType: ValueType<K>, valueType: ValueType<V>): MutableMap<K, V>

  /**
   * Flush allocated database resources and save data.
   */
  override fun close() = Unit
}