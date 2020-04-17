/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.ide.repositories

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.ide.AvailableIde

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
  fun fetchAvailableIde(ideVersion: IdeVersion): AvailableIde? {
    val fullIdeVersion = ideVersion.setProductCodeIfAbsent("IU")
    return fetchIndex().find { it.version == fullIdeVersion }
  }
}