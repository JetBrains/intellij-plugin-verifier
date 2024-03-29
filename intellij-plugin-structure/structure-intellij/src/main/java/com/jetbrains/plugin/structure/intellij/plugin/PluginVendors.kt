/*
 * Copyright 2000-2023 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

/**
 * Plugin Vendors that identify JetBrains-related plugins.
 *
 * @see com.intellij.ide.plugins.PluginManagerCore#isDeveloperByJetBrains
 */
object PluginVendors {
  private const val VENDOR_JETBRAINS = "JetBrains"
  private const val VENDOR_JETBRAINS_SRO = "JetBrains s.r.o."

  fun isDevelopedByJetBrains(plugin: IdePlugin) = isDevelopedByJetBrains(plugin.vendor)

  private fun isDevelopedByJetBrains(vendorString: String?): Boolean {
    if (vendorString == null) {
      return false
    }
    return vendorString.split(",")
      .map(String::trim)
      .filter(String::isNotEmpty)
      .any(::isVendorJetBrains)
  }

  private fun isVendorJetBrains(vendorItem: String?): Boolean {
    return VENDOR_JETBRAINS == vendorItem || VENDOR_JETBRAINS_SRO == vendorItem
  }
}