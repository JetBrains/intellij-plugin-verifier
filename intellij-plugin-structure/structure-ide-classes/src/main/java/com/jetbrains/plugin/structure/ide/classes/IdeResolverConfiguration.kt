package com.jetbrains.plugin.structure.ide.classes

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.ide.layout.MissingLayoutFileMode

data class IdeResolverConfiguration(
  val readMode: Resolver.ReadMode,
  val missingLayoutFileMode: MissingLayoutFileMode = MissingLayoutFileMode.SKIP_AND_WARN,
  val isCollectingStats: Boolean = false,
  /**
   * Indicate if layout components will be validated instead of being taken from an [com.jetbrains.plugin.structure.ide.Ide] instance.
   *
   * An [com.jetbrains.plugin.structure.ide.Ide] might contain a type-safe representation of the layout components
   * taken from the `product-info.json`.
   *
   * Forcing the `product-info.json` validation will reparse the layout components.
   */
  val forceProductInfoValidation: Boolean = false
)

