/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.warnings

import com.jetbrains.plugin.structure.base.utils.pluralize

data class MistakenlyBundledIdePackagesWarning(private val idePackages: List<String>) : CompatibilityWarning() {

  override val problemType: String
    get() = "Mistakenly bundled Ide packages warning"

  override val shortDescription
    get() = "Plugin bundles IDE packages"

  override val fullDescription = buildString {
    append("The plugin distribution bundles IDE ")
    append("package".pluralize(idePackages.size))
    append(" ")
    append(idePackages.joinToString { "'$it'" })
    append(". ")
    append("Bundling IDE packages is considered bad practice and may lead to sophisticated compatibility problems. ")
    append("Consider excluding these IDE packages from the plugin distribution. ")
    append("If your plugin depends on classes of an IDE bundled plugin, explicitly specify dependency on that plugin instead of bundling it. ")
  }
}