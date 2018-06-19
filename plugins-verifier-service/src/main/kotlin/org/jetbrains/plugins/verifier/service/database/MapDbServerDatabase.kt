package org.jetbrains.plugins.verifier.service.database

import com.jetbrains.pluginverifier.misc.createDir
import org.mapdb.DBMaker
import java.nio.file.Path

/**
 * Database implementation which uses the [MapDB library](https://github.com/jankotek/mapdb)
 * for storing the data.
 */
class MapDbServerDatabase(databasePath: Path) : ServerDatabase {
  private val serverDBFile = databasePath.createDir()
      .resolve("serverDB").toFile()

  private val serverDB = DBMaker
      .fileDB(serverDBFile)
      .checksumHeaderBypass()
      .closeOnJvmShutdown()
      .make()

  override fun <T> openOrCreateSet(setName: String, elementType: ValueType<T>): MutableSet<T> =
      serverDB
          .hashSet(setName, elementType.serializer)
          .createOrOpen()

  override fun <K, V> openOrCreateMap(mapName: String, keyType: ValueType<K>, valueType: ValueType<V>): MutableMap<K, V> =
      serverDB
          .hashMap(mapName, keyType.serializer, valueType.serializer)
          .createOrOpen()

  @Suppress("UNCHECKED_CAST")
  override fun <K> openOrCreateList(listName: String, keyType: ValueType<K>): MutableList<K> =
      serverDB.indexTreeList(listName, keyType.serializer)
          .createOrOpen() as MutableList<K>

  override fun close() = serverDB.close()
}
