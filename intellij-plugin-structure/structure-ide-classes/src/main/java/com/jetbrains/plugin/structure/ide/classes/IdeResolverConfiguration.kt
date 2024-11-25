package com.jetbrains.plugin.structure.ide.classes

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.ide.layout.MissingLayoutFileMode

data class IdeResolverConfiguration(val readMode: Resolver.ReadMode, val missingLayoutFileMode: MissingLayoutFileMode = MissingLayoutFileMode.SKIP_AND_WARN)

