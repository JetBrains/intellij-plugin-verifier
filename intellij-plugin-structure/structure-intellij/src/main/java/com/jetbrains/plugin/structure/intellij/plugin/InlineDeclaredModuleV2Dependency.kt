/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

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
 * The _depender content module ID is `com.intellij.modules.json`.
 *
 *
 * @param id the ID of the dependency declared in the content module CDATA declaration
 * @param isOptional whether the dependency is optional
 * @param contentModuleOwnerId the ID of the owner of the content model. Usually taken from the `plugin.xml`, from `<id>`.
 * @param dependerContentModuleId the ID of the content module that declares this dependency, taken from the
 *        `name` attribute of `<module>` element
 */
data class InlineDeclaredModuleV2Dependency(
  override val id: String,
  override var isOptional: Boolean = false,
  val contentModuleOwnerId: String,
  val dependerContentModuleId: String
) :
  PluginDependency {
  override val isModule = true

  override fun asOptional(): InlineDeclaredModuleV2Dependency = copy(isOptional = true)

  override fun toString() = "$id (module, v2, specified in content module '$dependerContentModuleId' of '$contentModuleOwnerId')"

  companion object {
    private const val UNKNOWN_INCLUDER = "Unknown Includer"

    @JvmStatic
    fun of(
      moduleId: String,
      moduleLoadingRule: ModuleLoadingRule,
      contentModuleOwner: IdePlugin,
      contentModuleReference: Module.InlineModule
    ): InlineDeclaredModuleV2Dependency =
      InlineDeclaredModuleV2Dependency(moduleId, !moduleLoadingRule.required, contentModuleOwner.id, contentModuleReference.name)

    private val IdePlugin.id: String
      get() = pluginId ?: pluginName ?: UNKNOWN_INCLUDER
  }
}

