package com.jetbrains.plugin.structure.classes.resolvers.jar

import com.jetbrains.plugin.structure.jar.PluginDescriptorResult
import java.nio.file.Path

interface DescriptorProvider {
  fun <T> resolveFromJar(jarFile: Path, descriptorPath: PathInJar, onSuccess: (PluginDescriptorResult.Found) -> T): T?
}