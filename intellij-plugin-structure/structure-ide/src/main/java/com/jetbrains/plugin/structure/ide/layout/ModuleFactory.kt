package com.jetbrains.plugin.structure.ide.layout

import com.jetbrains.plugin.structure.ide.layout.PluginWithArtifactPathResult.Failure
import com.jetbrains.plugin.structure.ide.layout.PluginWithArtifactPathResult.Success
import com.jetbrains.plugin.structure.intellij.beans.ModuleBean
import com.jetbrains.plugin.structure.intellij.platform.BundledModulesManager
import com.jetbrains.plugin.structure.intellij.platform.LayoutComponent
import com.jetbrains.plugin.structure.intellij.plugin.module.IdeModule
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path

private val LOG: Logger = LoggerFactory.getLogger(ModuleFactory::class.java)

internal class ModuleFactory(private val moduleLoader: LayoutComponentLoader, private val classpathProvider: ModuleClasspathProvider) : LayoutComponentFactory<LayoutComponent> {

  override fun read(
    layoutComponent: LayoutComponent,
    idePath: Path,
    ideVersion: IdeVersion,
    resourceResolver: ResourceResolver,
    moduleManager: BundledModulesManager
  ): PluginWithArtifactPathResult? {
    val moduleName = layoutComponent.name
    val moduleDescriptor = moduleManager.findModuleByName(moduleName)
    if (moduleDescriptor == null) {
      LOG.debug("No module descriptor found for {}", moduleName)
      return null
    }
    val loadingContext = getLoadingContext(moduleDescriptor, idePath)
    if (loadingContext == null) {
      LOG.debug("No module plugin descriptor found for {} in resource roots [{}]", moduleName,
          moduleDescriptor.resources.joinToString { it.path.toString() })
      return null
    }

    val moduleLoadingResult = moduleLoader.load(loadingContext.artifactPath, loadingContext.descriptorName, resourceResolver, ideVersion, layoutComponent.name)
    return when (moduleLoadingResult) {
      is Success -> {
        IdeModule
          .clone(moduleLoadingResult.plugin, moduleName)
          .apply {
            definedModules += moduleName
            classpath += classpathProvider.getClasspath(moduleName).map { idePath.resolve(it) }
            moduleDependencies += moduleDescriptor.dependencies
            resources += moduleDescriptor.resources
          }
          .asResult(moduleLoadingResult)
      }
      is Failure -> moduleLoadingResult
    }
  }

  private fun getLoadingContext(moduleDescriptor: ModuleBean, idePath: Path): ModuleLoadingContext? {
    val resourceRoot = moduleDescriptor.resources.firstOrNull() ?: return null
    val resourceRootPath = idePath.resolveFromModulesDir(resourceRoot.path)

    return ModuleLoadingContext(resourceRootPath, descriptorName = "${moduleDescriptor.name}.xml")
  }

  private fun Path.resolveFromModulesDir(p: Path): Path {
    return resolve("modules").resolve(p)
  }

  private fun IdeModule.asResult(resolvedIdeModule: Success): Success {
    return Success(resolvedIdeModule.pluginArtifactPath, this)
  }

}
