/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

/**
 * Represents a content module of IntelliJ plugin defined by a `<module` tag in a `<content>` tag.
 * @param name name of the module as specified in 'name' attribute of the `<module>` tag
 * @param namespace namespace of the module as specified in 'namespace' attribute of the `<module>` tag
 * @param actualNamespace equals to `namespace` if it's specified; if not, defaults to a synthetic implicit namespace
 *        for the plugin where the module is declared. Such implicit namespaces are used only inside dependencies on
 *        private modules from other modules of the same plugin, because if all modules are private, we don't require
 *        specifying the namespace explicitly.
 * @param loadingRule specifies how the module should be loaded by IntelliJ Platform
 */
sealed class Module(open val name: String, open val namespace: String?, open val actualNamespace: String, open val loadingRule: ModuleLoadingRule) {
  data class InlineModule(
    override val name: String,
    override val namespace: String?,
    override val actualNamespace: String,
    override val loadingRule: ModuleLoadingRule,
    val textContent: String
  ) : Module(name, namespace, actualNamespace, loadingRule) {
    override fun toString(): String = "$name (CDATA module, ${textContent.length} characters)"
  }

  data class FileBasedModule(
    override val name: String,
    override val namespace: String?,
    override val actualNamespace: String,
    override val loadingRule: ModuleLoadingRule,
    val configFile: String
  ) : Module(name, namespace, actualNamespace, loadingRule) {
    override fun toString(): String = "$name (file module, $configFile)"
  }
}
