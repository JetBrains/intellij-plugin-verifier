package com.jetbrains.plugin.structure.classes.resolvers.jar

import java.nio.file.Path

interface Descriptor

open class DescriptorReference(open val jarPath: Path, open val path: PathInJar) : Descriptor

data class PluginDescriptorReference(override val jarPath: Path, override val path: PathInJar) : DescriptorReference(jarPath, path)
data class ModuleDescriptorReference(override val jarPath: Path, override val path: PathInJar) : DescriptorReference(jarPath, path)

