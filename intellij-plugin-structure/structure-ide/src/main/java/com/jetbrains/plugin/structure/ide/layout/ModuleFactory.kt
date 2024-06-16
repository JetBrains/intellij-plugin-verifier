package com.jetbrains.plugin.structure.ide.layout

import com.jetbrains.plugin.structure.ide.ProductInfoBasedIdeManager.PluginWithArtifactPathResult
import com.jetbrains.plugin.structure.ide.ProductInfoBasedIdeManager.PluginWithArtifactPathResult.Failure
import com.jetbrains.plugin.structure.ide.ProductInfoBasedIdeManager.PluginWithArtifactPathResult.Success
import com.jetbrains.plugin.structure.intellij.beans.ModuleBean
import com.jetbrains.plugin.structure.intellij.platform.BundledModulesManager
import com.jetbrains.plugin.structure.intellij.plugin.module.IdeModule
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path

private val LOG: Logger = LoggerFactory.getLogger(ModuleFactory::class.java)

//FIXME duplicate code with com.jetbrains.plugin.structure.ide.layout.ProductModuleV2Factory
class ModuleFactory(private val moduleLoader: ModuleLoader, private val classpathProvider: ModuleClasspathProvider) {
  fun read(
    moduleName: String,
    idePath: Path,
    ideVersion: IdeVersion,
    platformResourceResolver: ResourceResolver,
    moduleManager: BundledModulesManager
  ): PluginWithArtifactPathResult? {
    val moduleDescriptor = moduleManager.findModuleByName(moduleName)
    if (moduleDescriptor == null) {
      LOG.atDebug().log("No module descriptor found for $moduleName")
      return null
    }
    val loadingContext = getLoadingContext(moduleDescriptor, idePath)
    if (loadingContext == null) {
      LOG.atDebug()
        .log("No module plugin descriptor found for $moduleName in resource roots [{}]",
          moduleDescriptor.resources.joinToString { it.path.toString() })
      return null
    }

    val moduleLoadingResult = moduleLoader.load(loadingContext.artifactPath, loadingContext.descriptorName, platformResourceResolver, ideVersion)
    return when (moduleLoadingResult) {
      is Success -> {
        IdeModule
          .clone(moduleLoadingResult.plugin, moduleName)
          .apply {
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
    //FIXME find constant for modules
    return resolve("modules").resolve(p)
  }

  private fun IdeModule.asResult(resolvedIdeModule: Success): Success {
    return Success(resolvedIdeModule.pluginArtifactPath, this)
  }

}
