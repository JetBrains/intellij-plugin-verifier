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
     * The options for the Verifier (excluded problems, etc).
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

/**
 * The exception signals that the plugin is failed to be loaded from the Repository.
 */
class RepositoryException(message: String, cause: Exception? = null) : IOException(message, cause)

object VParamsCreator {

  /**
   * Creates the Plugin instance by the given Plugin descriptor.
   * If the descriptor specifies the plugin build id, it firstly loads the
   * corresponding plugin build from the Repository.
   *
   * @param ideVersion the version of the compatible IDE. It's used if the plugin descriptor specifies the plugin id only.
   * @throws IncorrectPluginException if the specified plugin has incorrect structure
   * @throws RepositoryException if the plugin is not found in the Repository, or if the Repository doesn't respond.
   * @throws IOException if the plugin has a broken File.
   */
  @Throws(IncorrectPluginException::class, IOException::class)
  fun getPlugin(plugin: PluginDescriptor, ideVersion: IdeVersion): Plugin {
    return when (plugin) {
      is PluginDescriptor.ByInstance -> plugin.plugin //already created.
      is PluginDescriptor.ByFile -> {
        PluginManager.getInstance().createPlugin(plugin.file) //IncorrectPluginException, IOException
      }
      is PluginDescriptor.ByBuildId -> {
        val info = withRepositoryException { RepositoryManager.getInstance().findUpdateById(plugin.buildId) } ?: throw noSuchPlugin(plugin)
        val file = withRepositoryException { RepositoryManager.getInstance().getPluginFile(info) } ?: throw noSuchPlugin(plugin)
        return PluginManager.getInstance().createPlugin(file) //IncorrectPluginException, IOException
      }
      is PluginDescriptor.ByXmlId -> {
        val suitable: UpdateInfo?
        if (plugin.version != null) {
          val updates = withRepositoryException { RepositoryManager.getInstance().getAllCompatibleUpdatesOfPlugin(ideVersion, plugin.pluginId) } //IOException
          suitable = updates.find { plugin.version.equals(it.version) }
        } else {
          suitable = withRepositoryException { RepositoryManager.getInstance().getLastCompatibleUpdateOfPlugin(ideVersion, plugin.pluginId) } //IOException
        }
        if (suitable == null) {
          throw noSuchPlugin(plugin)
        }
        val file = withRepositoryException { RepositoryManager.getInstance().getPluginFile(suitable) } ?: throw noSuchPlugin(plugin)
        return PluginManager.getInstance().createPlugin(file) //IncorrectPluginException, IOException
      }
    }
  }

  private fun <T> withRepositoryException(block: () -> T): T {
    try {
      return block()
    } catch (e: Exception) {
      throw RepositoryException(e.message!!, e)
    }
  }

  private fun noSuchPlugin(plugin: PluginDescriptor): RepositoryException {
    val id: String = when (plugin) {
      is PluginDescriptor.ByBuildId -> plugin.buildId.toString()
      is PluginDescriptor.ByXmlId -> "${plugin.pluginId}${if (plugin.version != null) ":${plugin.version}" else "" }"
      is PluginDescriptor.ByFile -> "${plugin.file.name}"
      is PluginDescriptor.ByInstance -> plugin.plugin.toString()
    }
    return RepositoryException("Plugin $id is not found in the Plugin repository")
  }

  fun getIde(ideDescriptor: IdeDescriptor): Ide = when (ideDescriptor) {
    is IdeDescriptor.ByFile -> IdeManager.getInstance().createIde(ideDescriptor.file)
    is IdeDescriptor.ByInstance -> ideDescriptor.ide
  }

}