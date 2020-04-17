/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.repository

import java.net.URL

/**
 * Base interface for all entities that can be downloaded by [downloadUrl].
 */
interface Downloadable {

  /**
   * URL used to download this entity.
   */
  val downloadUrl: URL

}