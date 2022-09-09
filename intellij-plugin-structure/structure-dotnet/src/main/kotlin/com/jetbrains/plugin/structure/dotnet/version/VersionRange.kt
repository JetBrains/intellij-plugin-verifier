package com.jetbrains.plugin.structure.dotnet.version

import com.jetbrains.plugin.structure.base.utils.Version
import com.jetbrains.plugin.structure.dotnet.NugetSemanticVersion

interface  VersionRange<T: Version<T>>

data class ReSharperRange(val min: ReSharperVersion?, val isMinIncluded: Boolean, val max: ReSharperVersion?, val isMaxIncluded: Boolean): VersionRange<ReSharperVersion>

data class NugetSemanticVersionRange(val min: NugetSemanticVersion?, val isMinIncluded: Boolean, val max: NugetSemanticVersion?, val isMaxIncluded: Boolean): VersionRange<NugetSemanticVersion>