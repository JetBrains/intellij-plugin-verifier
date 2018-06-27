package com.jetbrains.pluginverifier.repository

import java.net.URL

/**
 * Base interface for all entities that have a browser URL
 */
interface Browseable {

  /**
   * URL used to open a browser.
   */
  val browserUrl: URL

}