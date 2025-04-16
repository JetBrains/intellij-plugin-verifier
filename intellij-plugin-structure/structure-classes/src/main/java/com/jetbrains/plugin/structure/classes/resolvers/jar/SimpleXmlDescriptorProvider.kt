package com.jetbrains.plugin.structure.classes.resolvers.jar

import com.jetbrains.plugin.structure.jar.PluginDescriptorResult
import org.apache.commons.io.input.NullInputStream
import java.nio.file.Path

object SimpleXmlDescriptorProvider : DescriptorProvider {
  override fun <T> resolveFromJar(jarFile: Path, descriptorPath: PathInJar, onSuccess: (PluginDescriptorResult.Found) -> T
  ): T? {
    val path = Path.of(jarFile.toString(), "!", descriptorPath.toString())
    val inputStream = NullInputStream()
    return onSuccess(PluginDescriptorResult.Found(path, inputStream))
  }
}
