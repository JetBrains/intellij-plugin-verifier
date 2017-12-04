package org.jetbrains.plugins.verifier.service.server.database

import com.jetbrains.pluginverifier.misc.closeLogged
import com.jetbrains.pluginverifier.misc.createDir
import org.mapdb.DBMaker
import org.mapdb.Serializer
import java.io.Closeable
import java.nio.file.Path

/**
 * Database implementation which uses the [MapDB library](https://github.com/jankotek/mapdb)
 * for storing the data.
 */
class MapDbServerDatabase(applicationHomeDir: Path) : ServerDatabase, Closeable {
  private val serverDBFile = applicationHomeDir.resolve("database").createDir().resolve("serverDB").toFile()

  private val serverDB = DBMaker
      .fileDB(serverDBFile)
      .checksumHeaderBypass()
      .closeOnJvmShutdown()
      .make()


  @Suppress("UNCHECKED_CAST")
  private fun <T> ValueType<T>.getSerializer(): Serializer<T> = when (this) {
    ValueType.STRING -> Serializer.STRING as Serializer<T>
    ValueType.INT -> Serializer.INTEGER as Serializer<T>
  }

  override fun <T> openOrCreateSet(setName: String, elementType: ValueType<T>) =
      serverDB
          .hashSet(setName, elementType.getSerializer<T>())
          .createOrOpen()

  override fun <K, V> openOrCreateMap(mapName: String, keyType: ValueType<K>, valueType: ValueType<V>) =
      serverDB
          .hashMap(mapName, keyType.getSerializer<K>(), valueType.getSerializer<V>())
          .createOrOpen()

  override fun close() {
    serverDB.closeLogged()
  }
}
