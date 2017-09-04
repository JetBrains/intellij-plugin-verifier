package com.jetbrains.plugin.structure.plugin

import com.jetbrains.plugin.structure.product.ProductVersion

interface Plugin {
  val pluginId: String?

  val pluginName: String?

  val pluginVersion: String?

  val sinceBuild: ProductVersion<*>?

  val untilBuild: ProductVersion<*>?

  val url: String?

  val changeNotes: String?

  val description: String?

  val vendor: String?

  val vendorEmail: String?

  val vendorUrl: String?
}
