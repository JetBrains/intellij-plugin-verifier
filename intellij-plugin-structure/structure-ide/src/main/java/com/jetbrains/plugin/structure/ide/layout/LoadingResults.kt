package com.jetbrains.plugin.structure.ide.layout

import com.jetbrains.plugin.structure.ide.layout.PluginWithArtifactPathResult.Failure
import com.jetbrains.plugin.structure.ide.layout.PluginWithArtifactPathResult.Success
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin

internal class LoadingResults() {
  constructor(results: List<PluginWithArtifactPathResult>) : this() {
    add(results)
  }

  private val _successes = mutableListOf<Success>()
  private val _failures = mutableListOf<Failure>()

  val successes: List<Success>
    get() = _successes

  val failures: List<Failure>
    get() = _failures

  val successfulPlugins: List<IdePlugin>
    get() = successes.map { it.plugin }

  fun add(results: List<PluginWithArtifactPathResult>) {
    for (result in results) {
      add(result)
    }
  }

  fun add(result: PluginWithArtifactPathResult): LoadingResults {
    when (result) {
      is Success -> _successes += result
      is Failure -> _failures += result
    }
    return this
  }
}
