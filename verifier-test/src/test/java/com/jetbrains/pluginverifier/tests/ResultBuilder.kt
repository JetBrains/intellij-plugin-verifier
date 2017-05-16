package com.jetbrains.pluginverifier.tests

import com.intellij.structure.ide.IdeVersion
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.*
import com.jetbrains.pluginverifier.utils.CmdOpts
import com.jetbrains.pluginverifier.utils.OptionsUtil
import java.io.File

object ResultBuilder {

  private val MOCK_IDE_VERSION = IdeVersion.createIdeVersion("IU-145.500")

  fun doIdeaAndPluginVerification(ideaFile: File, pluginFile: File): List<Result> {
    val ide: Ide = IdeManager.getInstance().createIde(ideaFile, MOCK_IDE_VERSION)
    val plugin = PluginManager.getInstance().createPlugin(pluginFile)
    val jdkPath = System.getenv("JAVA_HOME") ?: "/usr/lib/jvm/java-8-oracle"
    Resolver.createIdeResolver(ide).use { ideResolver ->
      Resolver.createPluginResolver(plugin).use { pluginResolver ->
        val pluginDescriptor = PluginDescriptor.ByInstance(plugin, pluginResolver)
        val ideDescriptor = IdeDescriptor.ByInstance(ide, ideResolver)
        val externalClassesPrefixes = OptionsUtil.getExternalClassesPrefixes(CmdOpts())
        val problemsFilter = OptionsUtil.getProblemsFilter(CmdOpts())
        val singleToCheck = listOf(pluginDescriptor to ideDescriptor)
        val vParams = VerifierParams(JdkDescriptor(File(jdkPath)), singleToCheck, externalClassesPrefixes, problemsFilter)
        return Verifier(vParams).verify()
      }
    }
  }

}

