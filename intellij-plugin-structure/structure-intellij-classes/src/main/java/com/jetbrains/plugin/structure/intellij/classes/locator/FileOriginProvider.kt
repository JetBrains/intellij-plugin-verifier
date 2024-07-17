package com.jetbrains.plugin.structure.intellij.classes.locator

import com.jetbrains.plugin.structure.classes.resolvers.FileOrigin
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import java.nio.file.Path

fun interface FileOriginProvider {
  fun getFileOrigin(idePlugin: IdePlugin, pluginFile: Path): FileOrigin
}

