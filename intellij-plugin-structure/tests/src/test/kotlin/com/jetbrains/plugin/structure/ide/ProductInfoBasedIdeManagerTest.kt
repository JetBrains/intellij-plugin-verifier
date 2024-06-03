package com.jetbrains.plugin.structure.ide

import org.junit.Test
import java.nio.file.Path

class ProductInfoBasedIdeManagerTest {
  @Test
  fun name() {
    val ideManager = ProductInfoBasedIdeManager()
    val ide = ideManager.createIde(Path.of("/Users/novotnyr/projects/jetbrains/platforms/IU-242.10180.25"))
    println(ide)
  }
}