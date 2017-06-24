package com.jetbrains.pluginverifier.tests

import com.intellij.structure.ide.IdeVersion
import com.jetbrains.pluginverifier.api.*
import com.jetbrains.pluginverifier.core.VerifierExecutor
import com.jetbrains.pluginverifier.ide.IdeCreator
import com.jetbrains.pluginverifier.options.CmdOpts
import com.jetbrains.pluginverifier.options.OptionsParser
import java.io.File

object ResultBuilder {

  private val MOCK_IDE_VERSION = IdeVersion.createIdeVersion("IU-145.500")

  fun doIdeaAndPluginVerification(ideaFile: File, pluginFile: File): Result {
    val ideDescriptor = IdeCreator.createByFile(ideaFile, MOCK_IDE_VERSION)
    val pluginCoordinate = PluginCoordinate.ByFile(pluginFile)
    val jdkPath = System.getenv("JAVA_HOME") ?: "/usr/lib/jvm/java-8-oracle"
    ideDescriptor.use {
      val externalClassesPrefixes = OptionsParser.getExternalClassesPrefixes(CmdOpts())
      val problemsFilter = OptionsParser.getProblemsFilter(CmdOpts())
      val verifierParams = VerifierParams(JdkDescriptor(File(jdkPath)), externalClassesPrefixes, problemsFilter)
      val verifier = VerifierExecutor(verifierParams)
      verifier.use {
        val results = verifier.verify(listOf(pluginCoordinate to ideDescriptor), DefaultProgress())
        return results.single()
      }
    }
  }

}

