/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.dependencies

import com.jetbrains.plugin.structure.base.plugin.PluginCreationResult
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.util.*

abstract class DependenciesTestBase {
  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  protected data class ModuleDescriptorSource(
    val archive: String,
    val module: String,
    val xml: String,
  )

  protected fun buildPluginWithResult(
    pluginDescriptor: String,
    moduleDescriptorSources: List<ModuleDescriptorSource> = emptyList(),
  ): PluginCreationResult<IdePlugin> {
    val pluginFile = buildZipFile(temporaryFolder.newFile("${UUID.randomUUID()}.zip").toPath()) {
      dir("plugin") {
        dir("lib") {
          zip("plugin.jar") {
            dir("META-INF") {
              file("plugin.xml", pluginDescriptor)
            }
          }
          moduleDescriptorSources
            .groupBy(ModuleDescriptorSource::archive)
            .forEach { (archiveName, descriptors) ->
              zip(archiveName) {
                descriptors.forEach { descriptor ->
                  file("${descriptor.module}.xml", descriptor.xml)
                }
              }
            }
        }
      }
    }
    return IdePluginManager.createManager().createPlugin(pluginFile, validateDescriptor = true)
  }

  protected fun buildPlugin(
    pluginDescriptor: String,
    moduleDescriptorSources: List<ModuleDescriptorSource> = emptyList(),
  ): IdePlugin {
    val result = buildPluginWithResult(pluginDescriptor, moduleDescriptorSources)
    if (result is PluginCreationSuccess) {
      return result.plugin
    }
    fail("Expected successful plugin creation, but got $result")
    throw AssertionError("Expected successful plugin creation")
  }
}