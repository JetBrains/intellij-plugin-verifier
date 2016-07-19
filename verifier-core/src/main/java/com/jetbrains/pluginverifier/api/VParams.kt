package com.jetbrains.pluginverifier.api

import com.intellij.structure.domain.Ide
import com.intellij.structure.domain.IdeManager
import com.intellij.structure.domain.IdeVersion
import com.intellij.structure.domain.Plugin
import com.intellij.structure.errors.IncorrectPluginException
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.format.UpdateInfo
import com.jetbrains.pluginverifier.misc.PluginCache
import com.jetbrains.pluginverifier.repository.RepositoryManager
import java.io.IOException

/**
 * The exception signals that the plugin is not found in the Repository.
 */
class UpdateNotFoundException(message: String, cause: Exception? = null) : RuntimeException(message, cause)

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
) {
  override fun toString(): String {
    val todo: Map<IdeDescriptor, List<PluginDescriptor>> = pluginsToCheck.groupBy { it.second }.mapValues { it.value.map { it.first } }
    return "Jdk = ${jdkDescriptor.presentableName()}; " +
        "(IDE -> [Plugins]) = [${todo.entries.joinToString { "${it.key.presentableName()} -> [${it.value.joinToString { it.presentableName() }}]" }}]; " +
        "vOptions: $options; "
  }
}

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
   * @throws UpdateNotFoundException if the plugin is not found in the Repository
   * @throws RuntimeException if the Repository doesn't respond
   */
  @Throws(IncorrectPluginException::class, IOException::class, UpdateNotFoundException::class, RuntimeException::class)
  fun getPlugin(plugin: PluginDescriptor, ideVersion: IdeVersion? = null): Plugin = when (plugin) {
    is PluginDescriptor.ByInstance -> plugin.plugin //already created.
    is PluginDescriptor.ByFile -> PluginCache.createPlugin(plugin.file) //IncorrectPluginException, IOException
    is PluginDescriptor.ByBuildId -> {
      val info = withRuntimeException { RepositoryManager.getInstance().findUpdateById(plugin.buildId) } ?: throw noSuchUpdate(plugin)
      val file = withRuntimeException { RepositoryManager.getInstance().getPluginFile(info) } ?: throw noSuchUpdate(plugin)
      PluginCache.createPlugin(file) //IncorrectPluginException, IOException
    }
    is PluginDescriptor.ByXmlId -> {
      val updates = withRuntimeException { RepositoryManager.getInstance().getAllCompatibleUpdatesOfPlugin(ideVersion!!, plugin.pluginId) } //IOException
      val suitable: UpdateInfo = updates.find { plugin.version.equals(it.version) } ?: throw noSuchUpdate(plugin)
      val file = withRuntimeException { RepositoryManager.getInstance().getPluginFile(suitable) } ?: throw noSuchUpdate(plugin)
      PluginCache.createPlugin(file) //IncorrectPluginException, IOException
    }
    is PluginDescriptor.ByUpdateInfo -> {
      val file = withRuntimeException { RepositoryManager.getInstance().getPluginFile(plugin.updateInfo) } ?: throw noSuchUpdate(plugin)
      PluginCache.createPlugin(file) //IncorrectPluginException, IOException
    }
  }

  private fun <T> withRuntimeException(block: () -> T): T {
    try {
      return block()
    } catch(ie: InterruptedException) {
      throw ie
    } catch(e: Exception) {
      throw RuntimeException(e.message ?: e.javaClass.name, e)
    }
  }

  private fun noSuchUpdate(plugin: PluginDescriptor): UpdateNotFoundException {
    return UpdateNotFoundException("Plugin ${plugin.presentableName()} is not found in the Plugin repository")
  }

  fun getIde(ideDescriptor: IdeDescriptor): Ide = when (ideDescriptor) {
    is IdeDescriptor.ByFile -> IdeManager.getInstance().createIde(ideDescriptor.file)
    is IdeDescriptor.ByInstance -> ideDescriptor.ide
    is IdeDescriptor.ByVersion -> TODO("Downloading the IDE by IdeVersion is not supported yet.")
    IdeDescriptor.AnyIde -> throw IllegalArgumentException()
  }


}
