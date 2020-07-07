/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.base.plugin.Plugin
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.jdom2.Document
import org.jdom2.Element
import java.nio.file.Path

interface IdePlugin : Plugin {
  val sinceBuild: IdeVersion?

  val untilBuild: IdeVersion?

  val extensions: Map<String, List<Element>>

  val dependencies: List<PluginDependency>

  val incompatibleModules: List<String>

  val definedModules: Set<String>

  val optionalDescriptors: List<OptionalPluginDescriptor>

  val underlyingDocument: Document

  val originalFile: Path?

  val productDescriptor: ProductDescriptor?

  val declaredThemes: List<IdeTheme>

  val useIdeClassLoader: Boolean

  val isImplementationDetail: Boolean

  fun isCompatibleWithIde(ideVersion: IdeVersion): Boolean
}
