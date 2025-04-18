/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.classes.resolvers

/**
 * Contains base names of resource bundles and their full names, for example
 * ```
 * messages.StandaloneBundle -> [messages.StandaloneBundle]
 * messages.LocalizedBundle -> [messages.LocalizedBundle, messages.LocalizedBundle_en, messages.LocalizedBundle_fr]
 * ```
 */
data class ResourceBundleNameSet(internal val bundleNames: Map<String, Set<String>>) {

  val baseBundleNames: Set<String> get() = bundleNames.keys

  val isEmpty: Boolean get() = bundleNames.isEmpty()

  operator fun get(baseName: String): Set<String> = bundleNames.getOrElse(baseName) { emptySet() }
}