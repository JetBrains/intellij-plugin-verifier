/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.base.plugin.Plugin
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.jdom2.Document
import org.jdom2.Element
import java.nio.file.Path

/**
 * Represents a plugin for IntelliJ Platform backed by a single `plugin.xml` descriptor.
 */
interface IdePlugin : Plugin {
  val sinceBuild: IdeVersion?

  val untilBuild: IdeVersion?

  /**
   * Declared extensions which access the corresponding extension points
   *
   * Key represents the fully qualified extension point, such as `com.intellij.appStarter`.
   * Value is a list of JDOM [elements][Element] corresponding to the extensions of this extension point.
   * Such elements contain raw extension properties, usually as XML attributes.
   */
  val extensions: Map<String, List<Element>>

  val appContainerDescriptor: IdePluginContentDescriptor

  val projectContainerDescriptor: IdePluginContentDescriptor

  val moduleContainerDescriptor: IdePluginContentDescriptor

  val dependencies: List<PluginDependency>

  val incompatibleModules: List<String>

  val definedModules: Set<String>

  val optionalDescriptors: List<OptionalPluginDescriptor>

  val modulesDescriptors: List<ModuleDescriptor>

  /**
   * Underlying plugin descriptor file parsed and resolved as XML Document.
   */
  val underlyingDocument: Document

  /**
   * Path to the plugin file.
   * Generally, it is either a JAR file path, a ZIP file path or a path to a directory.
   */
  val originalFile: Path?

  val productDescriptor: ProductDescriptor?

  val declaredThemes: List<IdeTheme>

  val useIdeClassLoader: Boolean

  val isImplementationDetail: Boolean

  val isV2: Boolean

  val hasDotNetPart: Boolean

  fun isCompatibleWithIde(ideVersion: IdeVersion): Boolean
}
