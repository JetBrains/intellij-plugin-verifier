package com.jetbrains.pluginverifier.tests

import com.intellij.structure.domain.Ide
import com.intellij.structure.domain.IdeManager
import com.intellij.structure.domain.IdeVersion
import com.intellij.structure.domain.PluginManager
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.*
import com.jetbrains.pluginverifier.utils.CmdOpts
import com.jetbrains.pluginverifier.utils.VOptionsUtil
import java.io.File

object TestResultBuilder {

  private val MOCK_IDE_VERSION = IdeVersion.createIdeVersion("IU-145.500")

  fun buildResult(ideaFile: File, pluginFile: File): VResults {
    val ide: Ide = IdeManager.getInstance().createIde(ideaFile, MOCK_IDE_VERSION)
    val plugin = PluginManager.getInstance().createPlugin(pluginFile)
    val jdkPath = System.getenv("JAVA_HOME") ?: "/usr/lib/jvm/java-8-oracle"
    Resolver.createIdeResolver(ide).use { ideResolver ->
      val pluginDescriptor = PluginDescriptor.ByInstance(plugin)
      val ideDescriptor = IdeDescriptor.ByInstance(ide, ideResolver)
      val vOptions = VOptionsUtil.parseOpts(CmdOpts())
      return VManager.verify(VParams(JdkDescriptor.ByFile(jdkPath), listOf(Pair<PluginDescriptor, IdeDescriptor>(pluginDescriptor, ideDescriptor)), vOptions, Resolver.getEmptyResolver(), true))
    }
  }

}

