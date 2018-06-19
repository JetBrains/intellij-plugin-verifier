package org.jetbrains.plugins.verifier.service.server

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.parameters.filtering.IgnoreCondition
import org.jetbrains.plugins.verifier.service.database.ServerDatabase
import org.jetbrains.plugins.verifier.service.database.ValueType
import java.io.Closeable
import java.util.*

/**
 * Data access object specific for the verifier service.
 */
class ServiceDAO(private val serverDatabase: ServerDatabase) : Closeable {
  private val properties = serverDatabase.openOrCreateMap("properties", ValueType.STRING, ValueType.STRING)

  private val _manuallyDownloadedIdes: MutableSet<IdeVersion> = Collections.synchronizedSet(
      serverDatabase.openOrCreateSet(
          "manuallyDownloadedIdes",
          ValueType.StringBased(
              toString = { it.asString() },
              fromString = { IdeVersion.createIdeVersion(it) }
          )
      )
  )

  private val _ignoreConditions: MutableList<IgnoreCondition> = Collections.synchronizedList(
      serverDatabase.openOrCreateList(
          "ignoredProblems",
          ValueType.StringBased(
              { it.serializeCondition() },
              { IgnoreCondition.parseCondition(it) }
          )
      )
  )

  /**
   * Contains set of IDE builds that were uploaded
   * to the service manually but not via automatic uploader.
   *
   * These IDEs must not be evicted from the verifier service
   * until manually requested.
   */
  val manuallyDownloadedIdes: Set<IdeVersion>
    @Synchronized
    get() = _manuallyDownloadedIdes

  @Synchronized
  fun addManuallyDownloadedIde(ideVersion: IdeVersion) {
    _manuallyDownloadedIdes.add(ideVersion)
  }

  @Synchronized
  fun removeManuallyDownloadedIde(ideVersion: IdeVersion) {
    _manuallyDownloadedIdes.remove(ideVersion)
  }

  /**
   * Contains conditions of compatibility problems to be ignored
   */
  val ignoreConditions: List<IgnoreCondition>
    @Synchronized
    get() = _ignoreConditions.toList()

  @Synchronized
  fun addIgnoreCondition(ignoreCondition: IgnoreCondition) {
    _ignoreConditions.add(ignoreCondition)
  }

  @Synchronized
  fun replaceIgnoreConditions(ignoreConditions: List<IgnoreCondition>) {
    _ignoreConditions.clear()
    _ignoreConditions.addAll(ignoreConditions)
  }

  fun setProperty(key: String, value: String): String? = properties.put(key, value)

  fun getProperty(key: String): String? = properties.get(key)

  override fun close() {
    serverDatabase.close()
  }

}