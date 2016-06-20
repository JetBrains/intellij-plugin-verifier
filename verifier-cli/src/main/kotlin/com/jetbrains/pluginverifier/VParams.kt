package com.jetbrains.pluginverifier

import com.intellij.structure.domain.*
import com.intellij.structure.errors.IncorrectPluginException
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.format.UpdateInfo
import com.jetbrains.pluginverifier.repository.RepositoryManager
import java.io.File
import java.io.IOException

/**
 * Accumulates parameters of the upcoming verification.
 *
 * @author Sergey Patrikeev
 */
data class VParams(

    /**
     * The JDK against which the plugins will be verified.
     */
    val runtimeDir: File,

    /**
     * The pairs of _(plugin, ide)_ which will be verified.
     */
    val pluginsToCheck: List<Pair<PluginDescriptor, IdeDescriptor>>,

    /**
     * The options for verifier (excluded problems, etc).
     */
    val options: VOptions,

    /**
     * The Resolver for external classes. The verification can refer to them.
     */
    val externalClassPath: Resolver = Resolver.getEmptyResolver()
)

/**
 * Descriptor of the plugin to be checked
 */
sealed class PluginDescriptor() {
  class ByXmlId(val pluginId: String, val version: String? = null) : PluginDescriptor() {
    override fun toString(): String {
      return "PluginDescriptor.ByXmlId(pluginId='$pluginId', version=$version)"
    }
  }

  class ByBuildId(val buildId: Int) : PluginDescriptor() {
    override fun toString(): String {
      return "PluginDescriptor.ByBuildId(buildId=$buildId)"
    }
  }

  class ByFile(val file: File) : PluginDescriptor() {
    constructor(path: String) : this(File(path))

    override fun toString(): String {
      return "PluginDescriptor.ByFile(file=$file)"
    }

  }

  class ByInstance(val plugin: Plugin) : PluginDescriptor() {
    override fun toString(): String {
      return "PluginDescriptor.ByInstance(plugin=$plugin)"
    }
  }
}

sealed class IdeDescriptor() {
  class ByFile(val file: File) : IdeDescriptor() {
    constructor(path: String) : this(File(path))

    override fun toString(): String {
      return "IdeDescriptor.ByFile(file=$file)"
    }

  }

  class ByInstance(val ide: Ide) : IdeDescriptor() {
    override fun toString(): String {
      return "IdeDescriptor.ByInstance(ide=$ide; file=${ide.idePath})"
    }
  }
}


object VParamsCreator {

  /**
   * @
   */
  @Throws(IncorrectPluginException::class, IOException::class)
  fun getPlugin(plugin: PluginDescriptor, ideVersion: IdeVersion): Plugin {
    return when (plugin) {
      is PluginDescriptor.ByInstance -> plugin.plugin
      is PluginDescriptor.ByFile -> {
        try {
          PluginManager.getInstance().createPlugin(plugin.file)
        } catch(e: IOException) {
          throw IncorrectPluginException("Failed to read the plugin content", e)
        }
      }
      is PluginDescriptor.ByBuildId -> {
        val info = RepositoryManager.getInstance().findUpdateById(plugin.buildId) ?: throw noSuchPlugin(plugin) //IOExceptions
        val file = RepositoryManager.getInstance().getPluginFile(info) ?: throw noSuchPlugin(plugin)
        return PluginManager.getInstance().createPlugin(file) //IncorrectPluginException
      }
      is PluginDescriptor.ByXmlId -> {
        val suitable: UpdateInfo?
        if (plugin.version != null) {
          val updates = RepositoryManager.getInstance().getAllCompatibleUpdatesOfPlugin(ideVersion, plugin.pluginId) //IOException
          suitable = updates.find { plugin.version.equals(it.version) }
        } else {
          suitable = RepositoryManager.getInstance().getLastCompatibleUpdateOfPlugin(ideVersion, plugin.pluginId) //IOException
        }
        if (suitable == null) {
          throw noSuchPlugin(plugin)
        }
        val file = RepositoryManager.getInstance().getPluginFile(suitable) //IOException
        file ?: throw noSuchPlugin(plugin)
        return PluginManager.getInstance().createPlugin(file) //IncorrectPluginException, IOException
      }
    }
  }

  private fun noSuchPlugin(plugin: PluginDescriptor): Exception {
    val p: String = when (plugin) {
      is PluginDescriptor.ByBuildId -> plugin.buildId.toString()
      is PluginDescriptor.ByXmlId -> "${plugin.pluginId}${if (plugin.version != null) ":${plugin.version}" else "" }"
      is PluginDescriptor.ByFile -> "${plugin.file.name}"
      is PluginDescriptor.ByInstance -> plugin.plugin.toString()
    }
    return IllegalArgumentException("Plugin $p is not found in the Plugin repository")
  }

  fun getIde(ideDescriptor: IdeDescriptor): Ide = when (ideDescriptor) {
    is IdeDescriptor.ByFile -> IdeManager.getInstance().createIde(ideDescriptor.file)
    is IdeDescriptor.ByInstance -> ideDescriptor.ide
  }

}