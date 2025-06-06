/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.intellij.plugin.Module.InlineModule
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.PluginId

/**
 * Dependency declared in a V2 module that is specified via `<![CDATA[...]]>` instruction.
 * Usually, this happens in the `plugin.xml` of a plugin, in the `<module name="...">` element content.
 *
 * For example:
 * ```
 * <idea-plugin
 *   <id>org.toml.lang</id>
 *   <content>
 *     <module name="intellij.toml.json"><![CDATA[
 *       <idea-plugin>
 *         <dependencies>
 *           <plugin id="com.intellij.modules.json" />
 * ```
 * This declares `id` of `com.intellij.modules.json` in the content module owner `org.toml.lang`.
 * The _depender content module ID_ is `com.intellij.modules.json`.
 *
 *
 * @param id the ID of the dependency declared in the content module CDATA declaration
 * @param isOptional whether the dependency is optional
 * @param contentModuleOwnerId the identifier of the content model’s owner, typically taken from the `<id>` element in plugin.xml.
 * @param dependerContentModuleId the ID of the content module that declares this dependency, taken from the
 *        `name` attribute of `<module>` element
 */
sealed class InlineDeclaredModuleV2Dependency(
  override val id: String,
  override var isOptional: Boolean = false,
  open val contentModuleOwnerId: String,
  open val dependerContentModuleId: String
) :
  PluginDependency {
  override val isModule = true

  data class Plugin(
    private val pluginId: PluginId,
    override var isOptional: Boolean = false,
    override val contentModuleOwnerId: String,
    override val dependerContentModuleId: String
  ) : InlineDeclaredModuleV2Dependency(pluginId, isOptional, contentModuleOwnerId, dependerContentModuleId) {

    override fun toString() =
      "dependency on plugin '$id' specified in content module '$dependerContentModuleId' of '$contentModuleOwnerId'"

    override fun asOptional() = copy(isOptional = true)
  }

  data class Module(
    private val moduleId: PluginId,
    override var isOptional: Boolean = false,
    override val contentModuleOwnerId: String,
    override val dependerContentModuleId: String
  ) : InlineDeclaredModuleV2Dependency(moduleId, isOptional, contentModuleOwnerId, dependerContentModuleId) {

    override fun toString() =
      "dependency on module '$id' specified in content module '$dependerContentModuleId' of '$contentModuleOwnerId'"

    override fun asOptional() = copy(isOptional = true)
  }

  companion object {
    private const val UNKNOWN_INCLUDER = "Unknown Includer"

    @JvmStatic
    fun onModule(
      moduleId: String,
      moduleLoadingRule: ModuleLoadingRule,
      contentModuleOwner: IdePlugin,
      contentModuleReference: InlineModule
    ): Module =
      Module(moduleId, !moduleLoadingRule.required, contentModuleOwner.id, contentModuleReference.name)

    @JvmStatic
    fun onPlugin(
      pluginId: PluginId,
      moduleLoadingRule: ModuleLoadingRule,
      contentModuleOwner: IdePlugin,
      contentModuleReference: InlineModule
    ): Plugin =
      Plugin(pluginId, !moduleLoadingRule.required, contentModuleOwner.id, contentModuleReference.name)

    private val IdePlugin.id: String
      get() = pluginId ?: pluginName ?: UNKNOWN_INCLUDER
  }
}

