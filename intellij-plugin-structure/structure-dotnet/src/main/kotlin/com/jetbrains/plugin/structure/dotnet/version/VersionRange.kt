package com.jetbrains.plugin.structure.dotnet.version

import com.jetbrains.plugin.structure.base.utils.Version

interface  VersionRange<T: Version<T>>

data class ReSharperRange(val min: ReSharperVersion?, val isMinIncluded: Boolean, val max: ReSharperVersion?, val isMaxIncluded: Boolean): VersionRange<ReSharperVersion>

data class WaveRange(val min: WaveVersion?, val isMinIncluded: Boolean, val max: WaveVersion?, val isMaxIncluded: Boolean): VersionRange<WaveVersion>