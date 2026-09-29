/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.intellij.beans.PluginBean
import com.jetbrains.plugin.structure.intellij.beans.PluginDependencyBean
import com.jetbrains.plugin.structure.intellij.plugin.ValidatableDescriptor.*

/**
 * [ValidatableDescriptor] over the JAXB [PluginBean]. A direct mapping: the bean already keeps every
 * value as written, including an explicit `optional="false"` on `<depends>`.
 */
class PluginBeanView(private val bean: PluginBean) : ValidatableDescriptor {
  override val id: String?
    get() = bean.id
  override val name: String?
    get() = bean.name
  override val version: String?
    get() = bean.pluginVersion
  override val url: String?
    get() = bean.url
  override val description: String?
    get() = bean.description
  override val changeNotes: String?
    get() = bean.changeNotes

  override val vendor: VendorView? by lazy {
    bean.vendor?.let { VendorView(it.name, it.url, it.email) }
  }

  override val ideaVersion: IdeaVersionView? by lazy {
    bean.ideaVersion?.let { IdeaVersionView(it.sinceBuild, it.untilBuild) }
  }

  override val productDescriptor: ProductDescriptorView? by lazy {
    bean.productDescriptor?.let { ProductDescriptorView(it.code, it.releaseDate, it.releaseVersion, it.eap, it.optional) }
  }

  override val dependencies: List<DependencyView> by lazy {
    bean.dependencies.orEmpty().map { it.toDependencyView() }
  }

  override val pluginAliases: List<String>
    get() = bean.pluginAliases.orEmpty()
}

internal fun PluginDependencyBean.toDependencyView() = DependencyView(dependencyId, optional, configFile)
