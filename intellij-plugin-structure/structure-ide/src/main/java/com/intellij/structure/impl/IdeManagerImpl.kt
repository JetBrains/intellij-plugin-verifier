package com.intellij.structure.impl

import com.google.common.base.Joiner
import com.google.common.io.Files
import com.intellij.structure.ide.Ide
import com.intellij.structure.ide.IdeManager
import com.intellij.structure.ide.IdeVersion
import com.intellij.structure.impl.domain.PluginManagerImpl
import com.intellij.structure.impl.utils.xml.JDOMXIncluder
import com.intellij.structure.impl.utils.xml.URLUtil
import com.intellij.structure.impl.utils.xml.XIncludeException
import com.intellij.structure.plugin.IdePlugin
import com.jetbrains.structure.plugin.PluginCreationFail
import com.jetbrains.structure.plugin.PluginCreationSuccess
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.TrueFileFilter
import org.apache.commons.io.filefilter.WildcardFileFilter
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.net.URL
import java.nio.charset.Charset
import java.util.*

/**
 * @author Sergey Patrikeev
 */
class IdeManagerImpl : IdeManager() {


  @Throws(IOException::class)
  override fun createIde(ideDir: File): Ide = createIde(ideDir, null)

  @Throws(IOException::class)
  override fun createIde(idePath: File, version: IdeVersion?): Ide {
    if (!idePath.exists()) {
      throw IllegalArgumentException("IDE file $idePath is not found")
    }
    val bundled = if (isSourceDir(idePath)) getDummyPluginsFromSources(idePath) else getIdeaPlugins(idePath)
    val ideVersion = version ?: if (isSourceDir(idePath)) readVersionFromSourcesDir(idePath) else readVersionFromBinaries(idePath)
    return IdeImpl(idePath, ideVersion, bundled)
  }

  private fun isMacOs(): Boolean {
    val osName = System.getProperty("os.name")?.toLowerCase() ?: return false
    return osName.contains("mac os x") || osName.contains("darwin") || osName.contains("osx")
  }

  @Throws(IOException::class)
  private fun readVersionFromBinaries(idePath: File): IdeVersion {
    if (isMacOs()) {
      val versionFile = File(idePath, "Resources/build.txt")
      if (versionFile.exists()) {
        return readBuildNumber(versionFile)
      }
    }
    val versionFile = File(idePath, "build.txt")
    if (!versionFile.exists()) {
      throw IllegalArgumentException(versionFile.toString() + " is not found")
    }
    return readBuildNumber(versionFile)
  }

  @Throws(IOException::class)
  private fun readVersionFromSourcesDir(idePath: File): IdeVersion {
    val buildFile = File(idePath, "build.txt")
    if (buildFile.exists()) {
      return readBuildNumber(buildFile)
    }
    val communityBuildFile = File(idePath, "community/build.txt")
    if (communityBuildFile.exists()) {
      return readBuildNumber(communityBuildFile)
    }
    throw IllegalArgumentException("Unable to find IDE version file (build.txt or community/build.txt)")
  }

  private class PluginFromSourcePathResolver internal constructor(private val myDescriptors: Map<String, File>) : JDOMXIncluder.DefaultPathResolver() {

    private fun resolveOutputDirectories(relativePath: String, base: String?): URL {
      val normalizedPath = if (relativePath.startsWith("./")) {
        "/META-INF/" + relativePath.substringAfter("./")
      } else {
        relativePath
      }

      val file = myDescriptors[normalizedPath]
      if (file != null) {
        try {
          return file.toURI().toURL()
        } catch (exc: Exception) {
          throw XIncludeException("File $file has an invalid URL presentation ", exc)
        }

      }
      throw XIncludeException("Unable to resolve " + normalizedPath + if (base != null) " against " + base else "")
    }

    override fun resolvePath(relativePath: String, base: String?): URL {
      try {
        val res = super.resolvePath(relativePath, base)
        //try the parent resolver
        URLUtil.openStream(res)
        return res
      } catch (e: IOException) {
        return resolveOutputDirectories(relativePath, base)
      }

    }
  }

  companion object {

    private val LOG = LoggerFactory.getLogger(IdeManagerImpl::class.java)

    @Throws(IOException::class)
    private fun readBuildNumber(versionFile: File): IdeVersion {
      val buildNumberString = Files.toString(versionFile, Charset.defaultCharset()).trim { it <= ' ' }
      return IdeVersion.createIdeVersion(buildNumberString)
    }

    fun isUltimate(ideaDir: File): Boolean = File(ideaDir, "community/.idea").isDirectory && getUltimateClassesRoot(ideaDir).isDirectory

    fun isCommunity(ideaDir: File): Boolean = File(ideaDir, ".idea").isDirectory && getCommunityClassesRoot(ideaDir).isDirectory

    fun getUltimateClassesRoot(ideaDir: File): File = File(ideaDir, "out/classes/production")

    fun getCommunityClassesRoot(ideaDir: File): File = File(ideaDir, "out/production")

    @Throws(IOException::class)
    private fun getDummyPluginsFromSources(ideaDir: File): List<IdePlugin> {
      if (isUltimate(ideaDir)) {
        return getDummyPlugins(getUltimateClassesRoot(ideaDir))
      } else if (isCommunity(ideaDir)) {
        return getDummyPlugins(getCommunityClassesRoot(ideaDir))
      } else {
        throw IllegalArgumentException("Incorrect IDEA structure: $ideaDir. It must be Community or Ultimate sources root with compiled class files.")
      }
    }

    private fun getDummyPlugins(root: File): List<IdePlugin> {
      val xmlFiles = FileUtils.listFiles(root, WildcardFileFilter("*.xml"), TrueFileFilter.TRUE)

      val pathResolver = getFromSourcesPathResolver(xmlFiles)
      return getDummyPlugins(xmlFiles, pathResolver)
    }

    private fun getDummyPlugins(xmlFiles: Collection<File>, pathResolver: JDOMXIncluder.PathResolver): List<IdePlugin> = xmlFiles
        .filter { "plugin.xml" == it.name }
        .map { it.absoluteFile.parentFile }
        .filter { "META-INF" == it.name && it.isDirectory && it.parentFile != null }
        .map { it.parentFile }
        .filter { it.isDirectory }
        .mapNotNull { createPluginByDir(it, pathResolver) }

    private fun createPluginByDir(pluginDirectory: File, pathResolver: JDOMXIncluder.PathResolver?): IdePlugin? {
      try {
        val pluginCreator = PluginManagerImpl(pathResolver).getPluginCreatorWithResult(pluginDirectory, false)
        pluginCreator.setOriginalFile(pluginDirectory)
        val creationResult = pluginCreator.pluginCreationResult
        return when (creationResult) {
          is PluginCreationSuccess -> creationResult.plugin
          is PluginCreationFail -> {
            val problems = creationResult.errorsAndWarnings
            LOG.debug("Failed to read plugin " + pluginDirectory + ". Problems: " + Joiner.on(", ").join(problems))
            null
          }
        }
      } catch (e: Exception) {
        LOG.debug("Unable to create plugin from sources: " + pluginDirectory, e)
      }

      return null
    }

    private fun getFromSourcesPathResolver(xmlFiles: Collection<File>): JDOMXIncluder.PathResolver {
      val xmlDescriptors = HashMap<String, File>()
      for (file in xmlFiles) {
        val path = file.absolutePath
        val parts = path.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (parts.size >= 2) {
          val key = "/" + parts[parts.size - 2] + "/" + parts[parts.size - 1]
          xmlDescriptors.put(key, file)
        }
      }
      return PluginFromSourcePathResolver(xmlDescriptors)
    }

    fun isSourceDir(dir: File): Boolean {
      return File(dir, ".idea").isDirectory
    }

    @Throws(IOException::class)
    private fun getIdeaPlugins(ideaDir: File): List<IdePlugin> {
      val pluginsDir = File(ideaDir, "plugins")
      val pluginsFiles = pluginsDir.listFiles() ?: return emptyList()
      return pluginsFiles.filter { it.isDirectory }.mapNotNull { createPluginByDir(it, null) }
    }
  }
}
