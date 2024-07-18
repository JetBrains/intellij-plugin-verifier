/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.jetbrains.pluginverifier.repository.repositories.dependency

import com.jetbrains.pluginverifier.repository.PluginInfo

/**
 * Plugin information that is resolved as a plugin dependency.
 */
class DependencyPluginInfo(val pluginInfo: PluginInfo) : PluginInfo(
  pluginInfo.pluginId,
  pluginInfo.pluginName,
  pluginInfo.version,
  pluginInfo.sinceBuild,
  pluginInfo.untilBuild,
  pluginInfo.vendor
)