/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.analysis

import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.results.problems.ClassNotFoundProblem
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.results.problems.UndeclaredPluginDependencyProblem
import com.jetbrains.pluginverifier.results.problems.UndeclaredPluginDependencyProblem.ApiElement.Class
import com.jetbrains.pluginverifier.verifiers.resolution.BinaryClassName
import com.jetbrains.pluginverifier.verifiers.resolution.toFullyQualifiedClassName

private const val JSON_PLUGIN_ID = "com.intellij.modules.json"

private const val JSON_PLUGIN_EXTRACTED_REASON = "JSON support has been extracted to a separate plugin."

object ExtractedJsonPluginAnalyzer {
  private val removedPackages = listOf(
    "com.intellij.json",
    "com.intellij.json.codeinsight",
    "com.intellij.json.highlighting",
    "com.intellij.json.psi",
    "com.jetbrains.jsonSchema"
  )

  private val removedClasses = listOf(
    "com.intellij.json.JsonElementTypes",
    "com.intellij.json.JsonFileType",
    "com.intellij.json.JsonLanguage",
    "com.intellij.json.JsonParserDefinition",
    "com.intellij.json.JsonTokenType"
  )

  fun analyze(
    ide: Ide,
    plugin: IdePlugin,
    compatibilityProblems: Collection<CompatibilityProblem>
  ): CompatibilityProblemChangeList {
    return if (!supports(ide)) {
      CompatibilityProblemChangeList()
    } else {
      CompatibilityProblemChangeList().also { problems ->
        compatibilityProblems.filterIsInstance<ClassNotFoundProblem>()
          .forEach { problem ->
            val className: BinaryClassName = problem.unresolved.className
            if (isRemovedClass(className) || isRemovedPackage(className)) {
              problems += undeclaredPluginDependency(className)
              problems -= problem
            }
          }
      }
    }
  }

  private fun supports(ide: Ide): Boolean =
    isAtLeastVersion(ide, "243")

  private fun isRemovedClass(className: BinaryClassName): Boolean =
    removedClasses.contains(className.toFullyQualifiedClassName())

  private fun isRemovedPackage(className: BinaryClassName): Boolean {
    val pkg = Package.of(className)
    val removedPkg = getRemovedPackage(pkg)
    return removedPkg != null
  }

  private fun getRemovedPackage(pkg: Package): Package? {
    val removedPackage = removedPackages.firstOrNull { it == pkg.name }
    return if (removedPackage != null) {
      Package(removedPackage)
    } else {
      pkg.parent.let {
        if (it == Package.ROOT) {
          null
        } else {
          getRemovedPackage(it)
        }
      }
    }
  }

  private fun undeclaredPluginDependency(className: BinaryClassName): UndeclaredPluginDependencyProblem {
    return UndeclaredPluginDependencyProblem(
      JSON_PLUGIN_ID,
      Class(className.toFullyQualifiedClassName()),
      JSON_PLUGIN_EXTRACTED_REASON
    )
  }

  private fun isAtLeastVersion(ide: Ide, expectedVersion: String): Boolean {
    return ide.version > IdeVersion.createIdeVersion(expectedVersion)
  }

  internal class Package(val name: String) {
    companion object {
      val ROOT = Package("")

      fun of(className: BinaryClassName): Package {
        return Package(className.packageName.toFullyQualifiedClassName())
      }
    }

    private val elements: List<String> = name.split(".")

    val parent: Package
      get() {
        val parentElements = elements.dropLast(1)
        return if (parentElements.isEmpty()) {
          ROOT
        } else {
          Package(parentElements.joinToString("."))
        }
      }

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (javaClass != other?.javaClass) return false

      other as Package

      return name == other.name
    }

    override fun hashCode(): Int = name.hashCode()
  }
}

private val BinaryClassName.packageName get() = substringBeforeLast('/', "")

