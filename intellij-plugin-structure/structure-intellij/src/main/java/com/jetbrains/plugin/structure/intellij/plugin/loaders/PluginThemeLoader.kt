/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.loaders

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdeTheme
import com.jetbrains.plugin.structure.intellij.plugin.loaders.PluginThemeLoader.Result.*
import com.jetbrains.plugin.structure.intellij.problems.UnableToFindTheme
import com.jetbrains.plugin.structure.intellij.problems.UnableToReadTheme
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver
import com.jetbrains.plugin.structure.intellij.verifiers.ProblemRegistrar
import java.nio.file.Path

private const val INTELLIJ_THEME_EXTENSION = "com.intellij.themeProvider"

class PluginThemeLoader {
  private val json = jacksonObjectMapper()

  fun load(plugin: IdePlugin, descriptorPath: Path, resolver: ResourceResolver, problemRegistrar: ProblemRegistrar): Result {
    val themePaths = plugin.extensions[INTELLIJ_THEME_EXTENSION]?.mapNotNull {
      it.getAttribute("path")?.value
    } ?: return NotFound

    val themes = mutableListOf<IdeTheme>()

    val problems = SimpleProblemRegistrar()
    for (themePath in themePaths) {
      val absolutePath = if (themePath.startsWith("/")) themePath else "/$themePath"
      when (val resolvedTheme = resolver.resolveResource(absolutePath, descriptorPath)) {
        is ResourceResolver.Result.Found -> resolvedTheme.load(descriptorPath, themePath, problems)?.let {
          themes += it
        }

        is ResourceResolver.Result.NotFound -> {
          problems.unableToFind(descriptorPath, themePath)
        }

        is ResourceResolver.Result.Failed -> {
          problems.unableToRead(descriptorPath, themePath)
        }
      }
    }
    return when {
      themes.isNotEmpty() && problems.isEmpty() -> Found(themes)
      problems.isNotEmpty() -> Failed.also { problems.copyTo(problemRegistrar) }
      else -> NotFound
    }
  }

  private fun ResourceResolver.Result.Found.load(descriptorPath: Path, themePath: String, problemRegistrar: ProblemRegistrar): IdeTheme? {
    return use {
      runCatching {
        json.readValue(it.resourceStream, IdeTheme::class.java)
      }.getOrElse {
        problemRegistrar.unableToRead(descriptorPath, themePath)
        null
      }
    }
  }

  private fun ProblemRegistrar.unableToRead(descriptorPath: Path, themePath: String) {
    registerProblem(UnableToReadTheme(descriptorPath.fileName.toString(), themePath))
  }

  private fun ProblemRegistrar.unableToFind(descriptorPath: Path, themePath: String) {
    registerProblem(UnableToFindTheme(descriptorPath.fileName.toString(), themePath))
  }

  private class SimpleProblemRegistrar(val problems: MutableList<PluginProblem> = mutableListOf()) : ProblemRegistrar {
    override fun registerProblem(problem: PluginProblem) {
      problems += problem
    }

    fun isNotEmpty() = problems.isNotEmpty()

    fun isEmpty() = problems.isEmpty()

    fun copyTo(other: ProblemRegistrar) {
      problems.forEach { other.registerProblem(it) }
    }
  }

  sealed class Result {
    data class Found(val themes: List<IdeTheme>) : Result()
    object Failed : Result()
    object NotFound : Result()
  }
}