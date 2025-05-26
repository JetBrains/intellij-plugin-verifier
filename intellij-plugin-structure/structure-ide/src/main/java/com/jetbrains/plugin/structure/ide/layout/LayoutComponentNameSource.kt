/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide.layout

/**
 * Provide layout component names from a specific resource.
 *
 * A usual resource that contains component name might be:
 *
 * * `product-info.json` and its layout components. Component names are provided in the `name` attribute.
 * * set of plugin descriptors (`plugin.xml` etc.) and its `id` or `name` element.
 */
interface LayoutComponentNameSource<S> {
  fun getNames(resource: S): List<String>
}