package org.jetbrains.plugins.verifier.service.server

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.jetbrains.plugins.verifier.service.database.ServerDatabase
import org.jetbrains.plugins.verifier.service.database.ValueType

/**
 * Data access object specific for the verifier service.
 */
class ServiceDAO(serverDatabase: ServerDatabase) {

  private val properties = serverDatabase.openOrCreateMap("properties", ValueType.STRING, ValueType.STRING)

  val manuallyDownloadedIdes: MutableSet<IdeVersion> = serverDatabase.openOrCreateSet(
      "manuallyDownloadedIdes",
      ValueType.StringBased(
          toString = { it.asString() },
          fromString = { IdeVersion.createIdeVersion(it) }
      )
  )

  fun setProperty(key: String, value: String): String? = properties.put(key, value)

  fun getProperty(key: String): String? = properties.get(key)

}