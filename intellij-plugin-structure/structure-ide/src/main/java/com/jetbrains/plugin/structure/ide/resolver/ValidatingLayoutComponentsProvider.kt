/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide.resolver

import com.jetbrains.plugin.structure.ide.layout.IdeRelativePath
import com.jetbrains.plugin.structure.ide.layout.LayoutComponents
import com.jetbrains.plugin.structure.ide.layout.LayoutComponentsProvider
import com.jetbrains.plugin.structure.ide.layout.MissingClasspathFileInLayoutComponentException
import com.jetbrains.plugin.structure.ide.layout.MissingLayoutFileMode
import com.jetbrains.plugin.structure.ide.layout.MissingLayoutFileMode.*
import com.jetbrains.plugin.structure.ide.layout.ResolvedLayoutComponent
import com.jetbrains.plugin.structure.ide.problem.IdeProblem
import com.jetbrains.plugin.structure.ide.problem.LayoutComponentHasNonExistentClasspath
import com.jetbrains.plugin.structure.intellij.platform.LayoutComponent
import com.jetbrains.plugin.structure.intellij.platform.ProductInfo
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path

private val LOG: Logger = LoggerFactory.getLogger(ValidatingLayoutComponentsProvider::class.java)

class ValidatingLayoutComponentsProvider(private val missingLayoutFileMode: MissingLayoutFileMode) :
  LayoutComponentsProvider {
  @Throws(MissingClasspathFileInLayoutComponentException::class)
  override fun resolveLayoutComponents(productInfo: ProductInfo, idePath: Path): LayoutComponents {
    val layoutComponents = LayoutComponents.of(idePath, productInfo)
    return if (missingLayoutFileMode == IGNORE) {
      layoutComponents
    } else {
      val validatedComponents = layoutComponents.partitionToSuccessesAndFailures()
      val acceptedComponents = mutableListOf<ResolvedLayoutComponent>()
      val ideProblems = mutableListOf<IdeProblem>()
      if (validatedComponents.hasFailures()) {
        ideProblems += validatedComponents.getIdeProblems()
        if (missingLayoutFileMode == FAIL) {
          throw MissingClasspathFileInLayoutComponentException.of(idePath, validatedComponents.failedComponents)
        }
        if (missingLayoutFileMode == SKIP_CLASSPATH) {
          acceptedComponents += validatedComponents.skipMissingClasspathElements()
        }
        logUnavailableClasspath(validatedComponents.failures)
      }
      LayoutComponents(validatedComponents.successes + acceptedComponents, ideProblems)
    }
  }

  private fun LayoutComponents.partitionToSuccessesAndFailures(): ValidatedLayoutComponents {
    val okComponents = mutableListOf<ResolvedLayoutComponent>()
    val failedComponents = mutableListOf<InvalidLayoutComponent>()
    forEach { component ->
      val missingPaths = component.resolveClasspaths().filterNot { it.exists }
      if (missingPaths.isEmpty()) {
        okComponents += component
      } else {
        failedComponents += InvalidLayoutComponent(component, missingPaths)
      }
    }
    return ValidatedLayoutComponents(okComponents, failedComponents)
  }

  private fun ValidatedLayoutComponents.getIdeProblems(): List<IdeProblem> {
    return failures.map {
      LayoutComponentHasNonExistentClasspath(it.component.name, it.missingClasspaths)
    }
  }

  private fun logUnavailableClasspath(invalidLayoutComponents: List<InvalidLayoutComponent>) {
    if (missingLayoutFileMode == SKIP_SILENTLY || !LOG.isWarnEnabled) return
    val logMsg = invalidLayoutComponents.joinToString("\n") { invalidComp ->
      val cp = invalidComp.missingClasspaths.map { it.relativePath }.joinToString(", ")
      val name = invalidComp.component.name
      "Layout component '${name}' has some nonexistent 'classPath' elements: '$cp'"
    }
    LOG.warn(logMsg)
  }

  private data class InvalidLayoutComponent(val component: ResolvedLayoutComponent, val missingClasspaths: List<IdeRelativePath>)

  private data class ValidatedLayoutComponents(
    val successes: List<ResolvedLayoutComponent>,
    val failures: List<InvalidLayoutComponent>,
  ) {
    val failedComponents: List<ResolvedLayoutComponent>
      get() = failures.map { it.component }

    fun hasFailures() = failures.isNotEmpty()

    fun skipMissingClasspathElements(): List<ResolvedLayoutComponent> =
      failedComponents.map { it.skipMissingClasspathElements() }

    private fun ResolvedLayoutComponent.skipMissingClasspathElements() = with(layoutComponent) {
      when (this) {
        is LayoutComponent.ModuleV2 -> copy(classPaths = existingClasspaths)
        is LayoutComponent.Plugin -> copy(classPaths = existingClasspaths)
        is LayoutComponent.ProductModuleV2 -> copy(classPaths = existingClasspaths)
        is LayoutComponent.PluginAlias -> this
      }
    }.let {
      ResolvedLayoutComponent(idePath, it)
    }

    private val ResolvedLayoutComponent.existingClasspaths
      get() = resolveClasspaths()
        .filter { it.exists }
        .map { it.relativePath.toString() }
  }
}