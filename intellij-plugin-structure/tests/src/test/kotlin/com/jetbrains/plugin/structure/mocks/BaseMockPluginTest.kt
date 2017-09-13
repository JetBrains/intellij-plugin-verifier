package com.jetbrains.plugin.structure.mocks

import org.junit.Assert.assertTrue
import java.io.File

abstract class BaseMockPluginTest {
  abstract fun getMockPluginBuildDirectory(): File

  fun getMockPluginFile(mockName: String): File {
    val buildDirectory = getMockPluginBuildDirectory()
    assertTrue("mock plugins build directory doesn't exist: " + buildDirectory.absolutePath, buildDirectory.exists())
    val pluginFile = File(buildDirectory, mockName)
    assertTrue("mock plugin '$mockName' is not found in " + buildDirectory.absolutePath, pluginFile.exists())
    return pluginFile
  }
}