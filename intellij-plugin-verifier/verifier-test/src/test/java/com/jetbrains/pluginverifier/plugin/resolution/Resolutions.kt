package com.jetbrains.pluginverifier.plugin.resolution

import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.problems.unwrapped
import com.jetbrains.pluginverifier.repository.PluginInfo
import org.junit.rules.TemporaryFolder
import java.nio.file.Path

internal fun TemporaryFolder.createJarPath(plugin: PluginInfo): Path {
  val jarFileName = with(plugin) {
    "${pluginId}-${version}.jar"
  }
  return newFile(jarFileName).toPath()
}

@Suppress("TestFunctionName")
internal fun PluginInfo(id: String, name: String, version: String, vendor: String = "JetBrains"): PluginInfo {
  return object : PluginInfo(id, name, version, sinceBuild = null, untilBuild = null, vendor = vendor) {
    // intentionally blank
  }
}

internal inline fun <reified T : PluginProblem> List<PluginProblem>.hasUnwrappedProblem(): Boolean =
  map {
    it.unwrapped
  }.filterIsInstance<T>()
    .isNotEmpty()