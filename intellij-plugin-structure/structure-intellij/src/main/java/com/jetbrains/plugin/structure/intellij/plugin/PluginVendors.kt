package com.jetbrains.plugin.structure.intellij.plugin

const val CORE_PLUGIN_ID = "com.intellij"
const val SPECIAL_IDEA_PLUGIN_ID = "IDEA CORE"
const val VENDOR_JETBRAINS = "JetBrains"
const val VENDOR_JETBRAINS_SRO = "JetBrains s.r.o."

/**
 * Plugin Vendors that identify JetBrains-related plugins.
 *
 * @see com.intellij.ide.plugins.PluginManagerCore#isDeveloperByJetBrains
 */
object PluginVendors {
  fun isDevelopedByJetBrains(plugin: IdePlugin): Boolean {
    return CORE_PLUGIN_ID == plugin.pluginId ||
      SPECIAL_IDEA_PLUGIN_ID == plugin.pluginId ||
      isDevelopedByJetBrains(plugin.vendor)
  }

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