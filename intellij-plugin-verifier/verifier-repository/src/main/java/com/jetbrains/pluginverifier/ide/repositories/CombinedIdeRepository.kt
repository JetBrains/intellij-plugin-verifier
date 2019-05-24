package com.jetbrains.pluginverifier.ide.repositories

class CombinedIdeRepository(private val ideRepositories: List<IdeRepository>) : IdeRepository {
  override fun fetchIndex() = ideRepositories.flatMap { it.fetchIndex() }
}