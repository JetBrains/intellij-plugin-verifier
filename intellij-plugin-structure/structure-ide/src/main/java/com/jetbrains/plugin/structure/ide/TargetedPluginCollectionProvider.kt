/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide

interface TargetedPluginCollectionProvider<S> : PluginCollectionProvider<S> {
  fun findPluginById(source: PluginCollectionSource<S, *>, pluginId: String): PluginLookupResult = PluginLookupResult.unsupported()

  fun findPluginByModule(source: PluginCollectionSource<S, *>, moduleId: String): PluginLookupResult = PluginLookupResult.unsupported()
}
