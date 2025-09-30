/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

/**
 * Represents a content module of IntelliJ plugin defined by a `<module` tag in a `<content>` tag.
 */
sealed class Module(open val name: String, open val namespace: String?, open val loadingRule: ModuleLoadingRule) {
  data class InlineModule(override val name: String, override val namespace: String?, override val loadingRule: ModuleLoadingRule, val textContent: String) : Module(name, namespace, loadingRule) {
    override fun toString(): String = "$name (CDATA module, ${textContent.length} characters)"
  }
  data class FileBasedModule(override val name: String, override val namespace: String?, override val loadingRule: ModuleLoadingRule, val configFile: String) : Module(name, namespace, loadingRule) {
    override fun toString(): String = "$name (file module, $configFile)"
  }
}
