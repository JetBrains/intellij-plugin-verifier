/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.loaders

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationResult
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.problems.PluginProblem.Level.ERROR
import com.jetbrains.plugin.structure.base.problems.UnableToReadDescriptor
import com.jetbrains.plugin.structure.base.utils.isJar
import com.jetbrains.plugin.structure.base.utils.toSystemIndependentName
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginImpl
import com.jetbrains.plugin.structure.intellij.plugin.InlineDeclaredModuleV2Dependency
import com.jetbrains.plugin.structure.intellij.plugin.Module.FileBasedModule
import com.jetbrains.plugin.structure.intellij.plugin.Module.InlineModule
import com.jetbrains.plugin.structure.intellij.plugin.ModuleDescriptor
import com.jetbrains.plugin.structure.intellij.plugin.ModuleLoadingRule
import com.jetbrains.plugin.structure.intellij.plugin.PluginCreator
import com.jetbrains.plugin.structure.intellij.plugin.PluginCreator.Companion.createInvalidPlugin
import com.jetbrains.plugin.structure.intellij.plugin.PluginCreator.Companion.createPlugin
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.plugin.structure.intellij.plugin.PluginLoader
import com.jetbrains.plugin.structure.intellij.plugin.descriptors.DescriptorResource
import com.jetbrains.plugin.structure.intellij.problems.AnyProblemToWarningPluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.problems.ModuleDescriptorProblem
import com.jetbrains.plugin.structure.intellij.problems.ModuleDescriptorResolutionProblem
import com.jetbrains.plugin.structure.intellij.problems.PluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver
import com.jetbrains.plugin.structure.intellij.utils.JDOMUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URI
import java.nio.file.Path

private val LOG: Logger = LoggerFactory.getLogger(ContentModuleLoader::class.java)

class ContentModuleLoader internal constructor(private val pluginLoader: PluginLoader) {
  internal fun resolveContentModules(pluginFile: Path, currentPlugin: PluginCreator, resourceResolver: ResourceResolver, problemResolver: PluginCreationResultResolver) {
    if (currentPlugin.isSuccess) {
      val contentModules = currentPlugin.plugin.contentModules
      for (module in contentModules) {
        when (module) {
          is FileBasedModule -> resolveFileBasedModule(
            module,
            pluginFile,
            currentPlugin,
            resourceResolver,
            problemResolver
          )

          is InlineModule -> resolveInlineModule(module, pluginFile, currentPlugin, resourceResolver)
        }
      }
    }
  }

  private fun resolveFileBasedModule(module: FileBasedModule, pluginFile: Path, currentPlugin: PluginCreator, resourceResolver: ResourceResolver, problemResolver: PluginCreationResultResolver) {
    val configFile = module.configFile
    val moduleCreator = pluginLoader.load(
      pluginFile,
      configFile,
      false,
      resourceResolver,
      currentPlugin,
      problemResolver
    )
    currentPlugin.addModuleDescriptor(module.name, module.loadingRule, configFile, moduleCreator)
  }

  private fun resolveInlineModule(module: InlineModule, pluginFile: Path, currentPlugin: PluginCreator, resourceResolver: ResourceResolver) {
    val moduleDescriptorResource = getDescriptorResource(module, pluginFile, currentPlugin.descriptorPath)
    val moduleCreator =
      loadModuleFromDescriptorResource(module.name, moduleDescriptorResource, currentPlugin, resourceResolver)
    currentPlugin.addModuleDescriptor(module, module.loadingRule, moduleDescriptorResource, moduleCreator)
  }

  internal fun PluginCreator.addModuleDescriptor(
    moduleName: String,
    loadingRule: ModuleLoadingRule,
    configurationFile: String,
    moduleCreator: PluginCreator
  ) {
    val pluginCreationResult = moduleCreator.pluginCreationResult
    if (pluginCreationResult is PluginCreationSuccess<IdePlugin>) {
      val module = pluginCreationResult.plugin

      plugin.addDependencies(module, loadingRule)
      plugin.modulesDescriptors.add(
        ModuleDescriptor(
          moduleName,
          loadingRule,
          module.dependencies,
          module,
          configurationFile
        )
      )
      plugin.definedModules.add(moduleName)

      mergeContent(module)
    } else {
      registerProblem(ModuleDescriptorResolutionProblem(moduleName, configurationFile, pluginCreationResult.errors))
    }
  }

  internal fun PluginCreator.addModuleDescriptor(
    inlineModuleReference: InlineModule,
    loadingRule: ModuleLoadingRule,
    moduleDescriptorResource: DescriptorResource,
    moduleCreator: PluginCreator
  ) {
    val pluginCreationResult = moduleCreator.pluginCreationResult
    if (pluginCreationResult is PluginCreationSuccess<IdePlugin>) {
      val moduleName = inlineModuleReference.name
      val inlineModule = pluginCreationResult.plugin

      plugin.addInlineModuleDependencies(inlineModuleReference, inlineModule, loadingRule)
      plugin.modulesDescriptors.add(
        ModuleDescriptor.of(
          moduleName,
          loadingRule,
          inlineModule,
          moduleDescriptorResource
        )
      )
      plugin.definedModules.add(moduleName)

      mergeContent(inlineModule)
    } else {
      registerProblem(ModuleDescriptorProblem(inlineModuleReference, pluginCreationResult.errors))
    }
  }

  private fun IdePluginImpl.addDependencies(module: IdePlugin, loadingRule: ModuleLoadingRule) {
    module.forEachDependencyNotIn(this) {
      val dependency = if (loadingRule.required) it else it.asOptional()
      dependencies += dependency
    }
  }

  private fun IdePluginImpl.addInlineModuleDependencies(
    inlineModuleReference: InlineModule,
    module: IdePlugin,
    loadingRule: ModuleLoadingRule
  ) {
    module.forEachDependencyNotIn(this) {
      dependencies += InlineDeclaredModuleV2Dependency.of(it.id, loadingRule, contentModuleOwner = this, inlineModuleReference)
    }
  }


  private fun IdePlugin.forEachDependencyNotIn(plugin: IdePlugin, dependencyHandler: (PluginDependency) -> Unit) {
    return dependencies
      .filter { dependency -> plugin.dependencies.none { it.id == dependency.id } }
      .forEach { dependencyHandler(it) }
  }


  private fun getDescriptorResource(module: InlineModule, pluginFile: Path, descriptorPath: String): DescriptorResource {
    // TODO descriptor path is not relative to the pluginFile JAR. See MP-7224
    val parentUriStr = if (pluginFile.isJar()) {
      "jar:" + pluginFile.toUri().toString() + "!" + descriptorPath.toSystemIndependentName()
    } else {
      pluginFile.toUri().toString() + "/" + descriptorPath.toSystemIndependentName()
    }
    val uriStr = parentUriStr + "#modules/" + module.name
    return DescriptorResource(module.textContent.byteInputStream(), URI(uriStr), URI(parentUriStr))
  }

  private fun loadModuleFromDescriptorResource(
    moduleId: String,
    descriptorResource: DescriptorResource,
    parentPlugin: PluginCreator? = null,
    resourceResolver: ResourceResolver
  ): PluginCreator {
    return descriptorResource.inputStream.use {
      try {
        val problemResolver = AnyProblemToWarningPluginCreationResultResolver
        val descriptorXml = JDOMUtil.loadDocument(it)
        createPlugin(
          descriptorResource,
          parentPlugin,
          descriptorXml,
          resourceResolver,
          problemResolver
        ).also { creator ->
          logPluginCreationWarnings(moduleId, creator)
        }
      } catch (e: IOException) {
        with(descriptorResource) {
          LOG.warn("Unable to read descriptor stream (source: '$uri')", e)
          val problem = UnableToReadDescriptor(fileName, e.localizedMessage)
          createInvalidPlugin(artifactFileName, fileName, problem)
        }
      }
    }
  }

  private fun logPluginCreationWarnings(pluginId: String, pluginCreator: PluginCreator) {
    val pluginCreationResult = pluginCreator.pluginCreationResult
    if (LOG.isDebugEnabled && pluginCreationResult is PluginCreationSuccess) {
      val warningMessage = pluginCreationResult.warnings.joinToString("\n") {
        it.message
      }
      LOG.debug("Plugin or module '$pluginId' has plugin problems: $warningMessage")
    }
  }

  private val PluginCreationResult<IdePlugin>.errors: List<PluginProblem>
    get() = when (this) {
      is PluginCreationSuccess -> emptyList()
      is PluginCreationFail -> this.errorsAndWarnings.filter { it.level === ERROR }
    }
}