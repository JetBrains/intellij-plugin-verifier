package com.jetbrains.plugin.structure.mocks

import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import java.nio.file.Path

data class MockIde(
  private val ideVersion: IdeVersion,
  private val idePath: Path,
  private val bundledPlugins: List<IdePlugin> = emptyList()
) : Ide() {

  override fun getIdePath() = idePath

  override fun getVersion() = ideVersion

  override fun getBundledPlugins() = bundledPlugins
}