package com.jetbrains.plugin.structure.mocks

import com.jetbrains.plugin.structure.base.plugin.Plugin
import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationResult
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.plugin.PluginManager
import com.jetbrains.plugin.structure.base.problems.ALLOWED_NAME_SYMBOLS
import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.rules.FileSystemType
import org.apache.commons.lang3.RandomStringUtils
import org.junit.Assert
import org.junit.Assert.assertEquals
import java.nio.file.Path

abstract class BasePluginManagerTest<P : Plugin, M : PluginManager<P>>(fileSystemType: FileSystemType) : BaseFileSystemAwareTest(fileSystemType) {

  abstract fun createManager(extractDirectory: Path): M

  fun createPluginSuccessfully(pluginFile: Path, pluginFactory: PluginFactory<P, M> = ::defaultPluginFactory): PluginCreationSuccess<P> {
    val pluginManager = createManager(extractedDirectory)
    val pluginCreationResult = createPlugin(pluginManager, pluginFile, pluginFactory)
    if (pluginCreationResult is PluginCreationFail) {
      Assert.fail(pluginCreationResult.errorsAndWarnings.joinToString())
    }
    return pluginCreationResult as PluginCreationSuccess<P>
  }

  fun assertProblematicPlugin(
    pluginFile: Path,
    expectedProblems: List<PluginProblem>,
    pluginFactory: PluginFactory<P, M> = ::defaultPluginFactory
  ): PluginCreationFail<P> {
    val pluginManager = createManager(extractedDirectory)
    val pluginCreationResult = createPlugin(pluginManager, pluginFile, pluginFactory)
    if (pluginCreationResult is PluginCreationSuccess) {
      Assert.fail("must have failed, but warnings: [${pluginCreationResult.warnings.joinToString()}]")
    }
    val creationFail = pluginCreationResult as PluginCreationFail
    val actualProblems = creationFail.errorsAndWarnings
    assertEquals(expectedProblems.toSet(), actualProblems.toSet())
    return creationFail
  }

  protected fun createPlugin(pluginManager: M, pluginArtifactPath: Path, pluginFactory: PluginFactory<P, M>): PluginCreationResult<P> {
    return pluginFactory.invoke(pluginManager, pluginArtifactPath)
  }

  protected fun defaultPluginFactory(pluginManager: M, pluginArtifactPath: Path): PluginCreationResult<P> {
    return pluginManager.createPlugin(pluginArtifactPath)
  }

  protected val extractedDirectory: Path by lazy {
    temporaryFolder.newFolder("extract")
  }

  protected fun getRandomNotAllowedNameSymbols(length: Int): String {
    val pattern = ALLOWED_NAME_SYMBOLS
    while (true) {
      val randomChar = RandomStringUtils.random(length)
      if (!randomChar.matches(pattern)) {
        return randomChar
      }
    }
  }

  protected fun getRandomAllowedNameSymbols(length: Int): String {
    val allowedCharacters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 .,+_-/:()#'[]|"
    return (1..length)
      .map { allowedCharacters.random() }
      .joinToString("")
  }
}

typealias PluginFactory<P, M> = M.(pluginArtifactPath: Path) -> PluginCreationResult<P>
