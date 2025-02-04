package com.jetbrains.plugin.structure.ide

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import java.nio.file.Path

class DispatchingIdeManager(configuration: IdeManagerConfiguration = IdeManagerConfiguration()) : IdeManager() {
  private val standardIdeManager = IdeManagerImpl()

  private val productInfoBasedIdeManager = ProductInfoBasedIdeManager(
    missingLayoutFileMode = configuration.missingLayoutFileMode,
    additionalPluginReader = UndeclaredInLayoutPluginReader(supportedProductCodes = setOf("AI")),
  )

  override fun createIde(idePath: Path): Ide = createIde(idePath, version = null)

  override fun createIde(idePath: Path, version: IdeVersion?): Ide {
    val ideManager = if (productInfoBasedIdeManager.supports(idePath)) {
      productInfoBasedIdeManager
    } else {
      standardIdeManager
    }
    return ideManager.createIde(idePath, version)
  }
}