package org.jetbrains.plugins.verifier.service.server

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.jetbrains.plugins.verifier.service.database.ServerDatabase
import org.jetbrains.plugins.verifier.service.database.ValueType

/**
 * Data access object specific for the verifier service.
 */
class ServiceDAO(serverDatabase: ServerDatabase) {

  private val properties = serverDatabase.openOrCreateMap("properties", ValueType.STRING, ValueType.STRING)

  private val manuallyUploadedIdes: MutableSet<String> = serverDatabase.openOrCreateSet("manuallyUploadedIdes", ValueType.STRING)

  fun addManuallyUploadedIde(ideVersion: IdeVersion) {
    manuallyUploadedIdes.add(ideVersion.toString())
  }

  fun getManuallyUploadedIdes(): Set<IdeVersion> =
      manuallyUploadedIdes.map { IdeVersion.createIdeVersion(it) }.toSet()

  fun removeManuallyUploadedIde(ideVersion: IdeVersion) {
    manuallyUploadedIdes.remove(ideVersion.toString())
  }


  fun setProperty(key: String, value: String): String? = properties.put(key, value)

  fun getProperty(key: String): String? = properties.get(key)

}