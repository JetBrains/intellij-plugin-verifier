package com.jetbrains.plugin.structure.base.plugin

interface Plugin {
  val pluginId: String?

  val pluginName: String?

  val pluginVersion: String?

  val url: String?

  val changeNotes: String?

  val description: String?

  val vendor: String?

  val vendorEmail: String?

  val vendorUrl: String?

  val icons: List<PluginIcon>
}
