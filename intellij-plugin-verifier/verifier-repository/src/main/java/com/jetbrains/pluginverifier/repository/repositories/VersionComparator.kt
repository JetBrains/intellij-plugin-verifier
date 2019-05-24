package com.jetbrains.pluginverifier.repository.repositories

import com.jetbrains.pluginverifier.misc.VersionComparatorUtil
import com.jetbrains.pluginverifier.repository.PluginInfo

val VERSION_COMPARATOR = compareBy<PluginInfo, String>(VersionComparatorUtil.COMPARATOR) { it.version }