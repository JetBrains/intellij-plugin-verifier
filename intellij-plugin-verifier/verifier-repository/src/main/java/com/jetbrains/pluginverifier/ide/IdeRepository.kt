package com.jetbrains.pluginverifier.ide

import com.jetbrains.plugin.structure.intellij.version.IdeVersion

/**
 * Provides lists of IDE builds available for downloading.
 */
interface IdeRepository {
  /**
   * Fetches available IDEs index from the data service.
   */
  @Throws(InterruptedException::class)
  fun fetchIndex(): List<AvailableIde>

  /**
   * Returns [AvailableIde] for this [ideVersion] if it is still available.
   */
  @Throws(InterruptedException::class)
  fun fetchAvailableIde(ideVersion: IdeVersion): AvailableIde?
}