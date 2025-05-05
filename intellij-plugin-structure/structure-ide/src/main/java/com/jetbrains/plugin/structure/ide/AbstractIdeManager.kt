/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.utils.exists
import com.jetbrains.plugin.structure.base.utils.readLines
import com.jetbrains.plugin.structure.base.utils.readText
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import java.nio.file.Path

abstract class AbstractIdeManager : IdeManager() {
  protected fun readBuildNumber(versionFile: Path): IdeVersion {
    val buildNumberString = versionFile.readText().trim()
    return IdeVersion.createIdeVersion(buildNumberString)
  }

  protected fun resolveProductSpecificVersion(idePath: Path, ideVersion: IdeVersion): IdeVersion {
    return if (ideVersion.productCode.isNotEmpty()) {
      ideVersion
    } else {
      //MPS builds' "build.txt" file does not specify product code.
      //MPS builds contain "build.number" file whose "build.number" key-value contains the product code.
      readIdeVersionFromBuildNumberFile(idePath) ?: ideVersion
    }
  }

  protected fun readIdeVersionFromBuildNumberFile(idePath: Path): IdeVersion? {
    val buildNumberFile = idePath.resolve("build.number")
    if (buildNumberFile.exists()) {
      val lines = buildNumberFile.readLines()
      for (line in lines) {
        if (line.startsWith("build.number=")) {
          return IdeVersion.createIdeVersionIfValid(line.substringAfter("build.number="))
        }
      }
    }
    return null
  }

  protected fun createBundledPluginExceptionally(
    idePath: Path,
    pluginFile: Path,
    pathResolver: ResourceResolver,
    descriptorPath: String,
    ideVersion: IdeVersion
  ): IdePlugin = when (val creationResult = IdePluginManager
    .createManager(pathResolver)
    .createBundledPlugin(pluginFile, ideVersion, descriptorPath)
  ) {
    is PluginCreationSuccess -> creationResult.plugin
    is PluginCreationFail -> throw InvalidIdeException(
      idePath,
      "Plugin '${idePath.relativize(pluginFile)}' is invalid: " +
        creationResult.errorsAndWarnings.filter { it.level == PluginProblem.Level.ERROR }.joinToString { it.message }
    )
  }
}