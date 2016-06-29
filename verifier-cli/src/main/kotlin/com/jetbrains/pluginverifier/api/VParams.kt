package com.jetbrains.pluginverifier.api

import com.intellij.structure.domain.*
import com.intellij.structure.errors.IncorrectPluginException
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.format.UpdateInfo
import com.jetbrains.pluginverifier.repository.RepositoryManager
import java.io.IOException

/**
 * The exception signals that the plugin is failed to be loaded from the Repository.
 */
class RepositoryException(message: String, cause: Exception? = null) : IOException(message, cause)

/**
 * Accumulates parameters of the upcoming verification.
 *
 * @author Sergey Patrikeev
 */
data class VParams(

    /**
     * The JDK against which the plugins will be verified.
     */
    val jdkDescriptor: JdkDescriptor,

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

object VParamsCreator {

  /**
   * Creates the [Resolver] by the given JDK descriptor.
   *
   * @throws IOException if the [Resolver] cannot be created.
   * @return [Resolver] of the JDK classes
   */
  @Throws(IOException::class)
  fun createJdkResolver(jdkDescriptor: JdkDescriptor): Resolver = when (jdkDescriptor) {
    is JdkDescriptor.ByFile -> Resolver.createJdkResolver(jdkDescriptor.file)
  }

  /**
   * Creates the Plugin instance by the given Plugin descriptor.
   * If the descriptor specifies the plugin build id, it firstly loads the
   * corresponding plugin build from the Repository.
   *
   * @param ideVersion the version of the compatible IDE. It's used if the plugin descriptor specifies the plugin id only.
   * @throws IncorrectPluginException if the specified plugin has incorrect structure
   * @throws IOException if the plugin has a broken File.
   * @throws RepositoryException if the plugin is not found in the Repository, or if the Repository doesn't respond.
   */
  @Throws(IncorrectPluginException::class, IOException::class)
  fun getPlugin(plugin: PluginDescriptor, ideVersion: IdeVersion? = null): Plugin = when (plugin) {
    is PluginDescriptor.ByInstance -> plugin.plugin //already created.
    is PluginDescriptor.ByFile -> PluginManager.getInstance().createPlugin(plugin.file) //IncorrectPluginException, IOException
    is PluginDescriptor.ByBuildId -> {
      val info = withRepositoryException { RepositoryManager.getInstance().findUpdateById(plugin.buildId) } ?: throw noSuchPlugin(plugin)
      val file = withRepositoryException { RepositoryManager.getInstance().getPluginFile(info) } ?: throw noSuchPlugin(plugin)
      PluginManager.getInstance().createPlugin(file) //IncorrectPluginException, IOException
    }
    is PluginDescriptor.ByXmlId -> {
      val updates = withRepositoryException { RepositoryManager.getInstance().getAllCompatibleUpdatesOfPlugin(ideVersion!!, plugin.pluginId) } //IOException
      val suitable: UpdateInfo = updates.find { plugin.version.equals(it.version) } ?: throw noSuchPlugin(plugin)
      val file = withRepositoryException { RepositoryManager.getInstance().getPluginFile(suitable) } ?: throw noSuchPlugin(plugin)
      PluginManager.getInstance().createPlugin(file) //IncorrectPluginException, IOException
    }
    is PluginDescriptor.ByUpdateInfo -> {
      val file = withRepositoryException { RepositoryManager.getInstance().getPluginFile(plugin.updateInfo) } ?: throw noSuchPlugin(plugin)
      PluginManager.getInstance().createPlugin(file)
    }
  }

  private fun <T> withRepositoryException(block: () -> T): T {
    try {
      return block()
    } catch(ie: InterruptedException) {
      throw ie
    } catch(e: Exception) {
      throw RepositoryException(e.message!!, e)
    }
  }

  private fun noSuchPlugin(plugin: PluginDescriptor): RepositoryException {
    val id: String = when (plugin) {
      is PluginDescriptor.ByBuildId -> plugin.buildId.toString()
      is PluginDescriptor.ByXmlId -> "${plugin.pluginId}:${plugin.version}"
      is PluginDescriptor.ByFile -> "${plugin.file.name}"
      is PluginDescriptor.ByInstance -> plugin.plugin.toString()
      is PluginDescriptor.ByUpdateInfo -> plugin.updateInfo.toString()
    }
    return RepositoryException("Plugin $id is not found in the Plugin repository")
  }

  fun getIde(ideDescriptor: IdeDescriptor): Ide = when (ideDescriptor) {
    is IdeDescriptor.ByFile -> IdeManager.getInstance().createIde(ideDescriptor.file)
    is IdeDescriptor.ByInstance -> ideDescriptor.ide
    is IdeDescriptor.ByVersion -> TODO()
  }



}
