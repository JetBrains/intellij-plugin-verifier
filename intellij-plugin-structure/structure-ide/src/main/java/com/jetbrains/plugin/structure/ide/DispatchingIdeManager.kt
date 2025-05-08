package com.jetbrains.plugin.structure.ide

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.util.*

internal const val COMPILED_IDE_MANAGER_CLASS_NAME = "com.jetbrains.plugin.structure.ide.jps.CompiledIdeManager"

private val LOG = LoggerFactory.getLogger(DispatchingIdeManager::class.java)

class DispatchingIdeManager(configuration: IdeManagerConfiguration = IdeManagerConfiguration()) : IdeManager() {
  private val standardIdeManager = IdeManagerImpl()

  private val productInfoBasedIdeManager = ProductInfoBasedIdeManager(
    missingLayoutFileMode = configuration.missingLayoutFileMode,
    additionalPluginReader = UndeclaredInLayoutPluginReader(  supportedProductCodes = setOf("AI", "CL")),
  )

  override fun createIde(idePath: Path): Ide = createIde(idePath, version = null)

  override fun createIde(idePath: Path, version: IdeVersion?): Ide {
    val compiledIdeManager = getCompiledIdeManager(idePath, version)
    val ideManager = when {
      compiledIdeManager != null -> compiledIdeManager.also { log("Compiled", idePath) }
      productInfoBasedIdeManager.supports(idePath) -> productInfoBasedIdeManager.also { log("Product Info", idePath) }
      else -> standardIdeManager.also { log("Standard", idePath) }
    }
    return ideManager.createIde(idePath, version)
  }

  private fun getCompiledIdeManager(idePath: Path, version: IdeVersion?): IdeManager? {
    return tryLoadCompiledIdeManager()?.takeIf { it.supports(idePath, version) }
  }

  private fun tryLoadCompiledIdeManager(): IdeManager? {
    val ideManagerLoader = ServiceLoader.load(IdeManager::class.java)
    return ideManagerLoader.firstOrNull { it.javaClass.name == COMPILED_IDE_MANAGER_CLASS_NAME }
  }

  private fun log(type: String, idePath: Path) {
    if (LOG.isDebugEnabled) {
      LOG.debug("Using {} IDE manager for {}", type, idePath)
    }
  }

}