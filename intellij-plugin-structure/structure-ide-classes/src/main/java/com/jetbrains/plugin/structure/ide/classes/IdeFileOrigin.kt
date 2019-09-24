package com.jetbrains.plugin.structure.ide.classes

import com.jetbrains.plugin.structure.classes.resolvers.FileOrigin

sealed class IdeFileOrigin : FileOrigin {
  override val parent: FileOrigin? = null

  object IdeLibDirectory : IdeFileOrigin()

  object RepositoryLibrary : IdeFileOrigin()
  object SourceLibDirectory : IdeFileOrigin()
  data class CompiledModule(val moduleName: String) : IdeFileOrigin()
}