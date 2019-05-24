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