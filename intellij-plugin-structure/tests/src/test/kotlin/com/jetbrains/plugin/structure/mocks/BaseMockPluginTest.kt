package com.jetbrains.plugin.structure.mocks

import org.junit.Assert.assertTrue
import java.io.File

abstract class BaseMockPluginTest {
  abstract fun getMockPluginBuildDirectory(): File

  fun getMockPluginFile(mockName: String): File {
    val pluginFile = File(getMockPluginBuildDirectory(), mockName)
    assertTrue("mock plugin '$mockName' is not found in " + getMockPluginBuildDirectory().absolutePath, pluginFile.exists())
    return pluginFile
  }
}