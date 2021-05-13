/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.ide.repositories

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.ide.AvailableIde
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class CompositeIdeRepository(private val ideRepositories: List<IdeRepository>) : IdeRepository {

  private companion object {
    val LOG: Logger = LoggerFactory.getLogger(CompositeIdeRepository::class.java)
  }

  override fun fetchIndex(): List<AvailableIde> {
    val versionToIde = hashMapOf<IdeVersion, AvailableIde>()
    for (ideRepository in ideRepositories) {
      val index = try {
        ideRepository.fetchIndex()
      } catch (e: Exception) {
        LOG.warn("Failed to request index from $ideRepository: ${e.message}")
        continue
      }
      for (availableIde in index) {
        val ide = versionToIde[availableIde.version]
        if (ide == null || availableIde.isRelease) {
          versionToIde[availableIde.version] = availableIde
        }
      }
    }
    return versionToIde.values.toList()
  }
}