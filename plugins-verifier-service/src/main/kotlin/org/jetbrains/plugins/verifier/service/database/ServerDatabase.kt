package org.jetbrains.plugins.verifier.service.database

import java.io.Closeable

/**
 * Server database allows to persist data between the server start-ups.
 *
 * The database must be [closed] [close] on the server shutdown.
 */
interface ServerDatabase : Closeable {

  /**
   * Provides a set of objects with the specified [elementType]
   * that will be persisted on the database shutdown.
   */
  fun <T> openOrCreateSet(setName: String, elementType: ValueType<T>): MutableSet<T>

  /**
   * Provides a map of objects with key types [keyType] and value types [valueType]
   * that will be persisted on the database shutdown.
   */
  fun <K, V> openOrCreateMap(mapName: String, keyType: ValueType<K>, valueType: ValueType<V>): MutableMap<K, V>

  /**
   * Flush allocated database resources and save data.
   */
  override fun close() = Unit
}