/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

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

  val thirdPartyDependencies: List<ThirdPartyDependency>
}
