/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.plugins.verifier.service.server

import com.jetbrains.pluginverifier.filtering.IgnoreCondition
import org.jetbrains.plugins.verifier.service.database.ServerDatabase
import org.jetbrains.plugins.verifier.service.database.ValueType
import java.io.Closeable
import java.util.*

/**
 * Data access object specific for the verifier service.
 */
class ServiceDAO(private val serverDatabase: ServerDatabase) : Closeable {
  private val properties = serverDatabase.openOrCreateMap("properties", ValueType.STRING, ValueType.STRING)

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

  fun getProperty(key: String): String? = properties[key]

  override fun close() {
    serverDatabase.close()
  }

}