package com.jetbrains.plugin.structure.ide.classes

import com.jetbrains.plugin.structure.classes.resolvers.ClassFileOrigin

sealed class IdeClassFileOrigin : ClassFileOrigin {
  override val parent: ClassFileOrigin? = null

  object IdeLibDirectory : IdeClassFileOrigin()

  object RepositoryLibrary : IdeClassFileOrigin()
  object SourceLibDirectory : IdeClassFileOrigin()
  data class CompiledModule(val moduleName: String) : IdeClassFileOrigin()
}