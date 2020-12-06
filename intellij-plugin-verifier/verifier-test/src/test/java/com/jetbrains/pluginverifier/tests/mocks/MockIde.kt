package com.jetbrains.pluginverifier.tests.mocks

import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import java.nio.file.Path
import java.nio.file.Paths

data class MockIde(
  private val ideVersion: IdeVersion,
  private val idePath: Path = Paths.get(""),
  private val bundledPlugins: List<IdePlugin> = emptyList()
) : Ide() {

  override fun getIdePath() = idePath

  override fun getVersion() = ideVersion

  override fun getBundledPlugins() = bundledPlugins

}