package com.jetbrains.pluginverifier.ide.repositories

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.ide.AvailableIde

class CompositeIdeRepository(private val ideRepositories: List<IdeRepository>) : IdeRepository {
  override fun fetchIndex(): List<AvailableIde> {
    val versionToIde = hashMapOf<IdeVersion, AvailableIde>()
    for (ideRepository in ideRepositories) {
      for (availableIde in ideRepository.fetchIndex()) {
        val ide = versionToIde[availableIde.version]
        if (ide == null || availableIde.isRelease) {
          versionToIde[availableIde.version] = availableIde
        }
      }
    }
    return versionToIde.values.toList()
  }
}