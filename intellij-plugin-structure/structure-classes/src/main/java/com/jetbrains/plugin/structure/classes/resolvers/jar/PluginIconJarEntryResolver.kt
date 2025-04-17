package com.jetbrains.plugin.structure.classes.resolvers.jar

import com.jetbrains.plugin.structure.base.utils.componentAt
import com.jetbrains.plugin.structure.base.utils.occurrences
import com.jetbrains.plugin.structure.jar.JarEntryResolver
import com.jetbrains.plugin.structure.jar.PathInJar
import com.jetbrains.plugin.structure.jar.replaceCharacter
import java.io.File
import java.nio.CharBuffer
import java.util.zip.ZipEntry

private const val PATH_SEPARATOR = '/'

//FIXME duplicate with Jar
private val NO_SUFFIX: CharBuffer = CharBuffer.allocate(0)


class PluginIconJarEntryResolver : JarEntryResolver<CharSequence> {
  override val key: JarEntryResolver.Key<CharSequence> = JarEntryResolver.Key("PluginIcon", CharSequence::class.java)

  override fun resolve(path: PathInJar, zipEntry: ZipEntry): CharSequence? {
    val descriptorPath = path.replaceCharacter(File.separatorChar, PATH_SEPARATOR, NO_SUFFIX)
    if (descriptorPath.startsWith("META-INF/") && descriptorPath.occurrences(PATH_SEPARATOR) == 1
      && descriptorPath.endsWith(".svg", ignoreCase = true)
      ) {
      val iconFileName = descriptorPath.componentAt(1, PATH_SEPARATOR)
      if (iconFileName != null) {
        return iconFileName
      }
    }
    return null
  }
}