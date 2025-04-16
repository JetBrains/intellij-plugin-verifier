package com.jetbrains.plugin.structure.classes.resolvers.jar

import java.nio.file.Path

interface Descriptor

class DescriptorReference(val jarPath: Path, val path: PathInJar) : Descriptor