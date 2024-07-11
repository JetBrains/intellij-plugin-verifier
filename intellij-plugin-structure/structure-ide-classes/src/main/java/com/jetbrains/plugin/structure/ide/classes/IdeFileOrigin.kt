/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide.classes

import com.jetbrains.plugin.structure.classes.resolvers.FileOrigin
import com.jetbrains.plugin.structure.ide.Ide

sealed class IdeFileOrigin : FileOrigin {
  override val parent: FileOrigin? = null

  data class IdeLibDirectory(override val ide: Ide) : IdeAwareFileOrigin(ide)
  data class RepositoryLibrary(override val ide: Ide) : IdeAwareFileOrigin(ide)
  data class SourceLibDirectory(override val ide: Ide) : IdeAwareFileOrigin(ide)
  data class CompiledModule(override val ide: Ide, val moduleName: String) : IdeAwareFileOrigin(ide)

  class BundledPlugin() : IdeFileOrigin()

  abstract class IdeAwareFileOrigin(open val ide: Ide): IdeFileOrigin()
}