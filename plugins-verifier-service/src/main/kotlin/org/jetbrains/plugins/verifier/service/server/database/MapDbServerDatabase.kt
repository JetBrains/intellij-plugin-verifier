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

  private val persistentProperties = serverDB
      .hashMap("properties", Serializer.STRING, Serializer.STRING)
      .createOrOpen()

  override fun setProperty(key: String, value: String): String? = persistentProperties.put(key, value)

  override fun getProperty(key: String): String? = persistentProperties.get(key)

  override fun close() {
    persistentProperties.closeLogged()
    serverDB.closeLogged()
  }
}
