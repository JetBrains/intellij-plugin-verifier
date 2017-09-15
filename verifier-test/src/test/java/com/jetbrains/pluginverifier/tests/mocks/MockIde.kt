package com.jetbrains.pluginverifier.tests.mocks

import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import java.io.File

data class MockIde(
    private val ideVersion: IdeVersion,
    private val idePath: File = File(""),
    private val bundledPlugins: List<IdePlugin> = emptyList(),
    private val customPlugins: List<IdePlugin> = emptyList()
) : Ide() {

  override fun getIdePath(): File = idePath

  override fun getVersion(): IdeVersion = ideVersion

  override fun getExpandedIde(plugin: IdePlugin): Ide {
    val newCustomPlugins = ArrayList(customPlugins)
    newCustomPlugins.add(plugin)
    return copy(customPlugins = newCustomPlugins)
  }

  override fun getCustomPlugins(): List<IdePlugin> = customPlugins

  override fun getBundledPlugins(): List<IdePlugin> = bundledPlugins
}