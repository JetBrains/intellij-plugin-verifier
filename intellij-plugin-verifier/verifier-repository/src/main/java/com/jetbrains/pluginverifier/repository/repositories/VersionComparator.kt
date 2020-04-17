/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.repository.repositories

import com.jetbrains.plugin.structure.ide.VersionComparatorUtil
import com.jetbrains.pluginverifier.repository.PluginInfo

val VERSION_COMPARATOR = compareBy<PluginInfo, String>(VersionComparatorUtil.COMPARATOR) { it.version }