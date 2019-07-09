package com.jetbrains.plugin.structure.intellij.classes.locator

import com.jetbrains.plugin.structure.classes.resolvers.ClassFileOrigin
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin

sealed class PluginClassFileOrigin : ClassFileOrigin {
  override val parent: ClassFileOrigin? = null

  abstract val idePlugin: IdePlugin

  data class LibDirectory(override val idePlugin: IdePlugin) : PluginClassFileOrigin()
  data class ClassesDirectory(override val idePlugin: IdePlugin) : PluginClassFileOrigin()
  data class SingleJar(override val idePlugin: IdePlugin) : PluginClassFileOrigin()
  data class CompileServer(override val idePlugin: IdePlugin) : PluginClassFileOrigin()
}