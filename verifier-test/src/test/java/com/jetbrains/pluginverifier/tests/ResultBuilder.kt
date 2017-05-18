package com.jetbrains.pluginverifier.tests

import com.intellij.structure.ide.IdeVersion
import com.jetbrains.pluginverifier.api.*
import com.jetbrains.pluginverifier.ide.IdeCreator
import com.jetbrains.pluginverifier.repository.IdleFileLock
import com.jetbrains.pluginverifier.utils.CmdOpts
import com.jetbrains.pluginverifier.utils.OptionsUtil
import java.io.File

object ResultBuilder {

  private val MOCK_IDE_VERSION = IdeVersion.createIdeVersion("IU-145.500")

  fun doIdeaAndPluginVerification(ideaFile: File, pluginFile: File): List<VerificationResult> {
    val createIdeResult = IdeCreator.createByFile(ideaFile, MOCK_IDE_VERSION)
    val pluginDescriptor = PluginDescriptor.ByFileLock(IdleFileLock(pluginFile))
    val jdkPath = System.getenv("JAVA_HOME") ?: "/usr/lib/jvm/java-8-oracle"
    createIdeResult.use {
      val externalClassesPrefixes = OptionsUtil.getExternalClassesPrefixes(CmdOpts())
      val problemsFilter = OptionsUtil.getProblemsFilter(CmdOpts())
      val singleToCheck = listOf(pluginDescriptor to IdeDescriptor.ByInstance(createIdeResult))
      val vParams = VerifierParams(JdkDescriptor(File(jdkPath)), singleToCheck, externalClassesPrefixes, problemsFilter)
      return Verifier(vParams).verify()
    }
  }

}

