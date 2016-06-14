package com.jetbrains.pluginverifier

import com.intellij.structure.domain.*
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.format.UpdateInfo
import com.jetbrains.pluginverifier.repository.RepositoryManager
import java.io.File

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
    val pluginsToCheck: List<PluginOnIde>,

    /**
     * The options for verifier (excluded problems, etc).
     */
    val options: VOptions,

    /**
     * The Resolver for external classes. The verification can refer to them.
     */
    val externalClassPath: Resolver = Resolver.getEmptyResolver()
)

data class PluginOnIde(val pluginDescriptor: PluginDescriptor, val plugin: Plugin, val ideDescriptor: IdeDescriptor, val ide: Ide) {
  companion object {
    fun create(pluginDescriptor: PluginDescriptor, ideDescriptor: IdeDescriptor) = VParamsCreator.create(pluginDescriptor, ideDescriptor)
  }
}

/**
 * Descriptor of the plugin to be checked
 */
sealed class PluginDescriptor() {
  class ByXmlId(val pluginId: String, val version: String?) : PluginDescriptor()
  class ByBuildId(val buildId: Int) : PluginDescriptor()
  class ByFile(val file: File) : PluginDescriptor() {
    constructor(path: String) : this(File(path))
  }

  class ByInstance(val plugin: Plugin) : PluginDescriptor()
}

sealed class IdeDescriptor() {
  class ByFile(val file: File) : IdeDescriptor() {
    constructor(path: String) : this(File(path))
  }

  class ByInstance(val ide: Ide) : IdeDescriptor()
}


private object VParamsCreator {

  fun create(pluginDescriptor: PluginDescriptor, ideDescriptor: IdeDescriptor): PluginOnIde {
    val ide = getIde(ideDescriptor)
    val plugin = getPlugin(pluginDescriptor, ide.version)
    return PluginOnIde(pluginDescriptor, plugin, ideDescriptor, ide)
  }

  private fun getPlugin(plugin: PluginDescriptor, ideVersion: IdeVersion): Plugin {
    return when (plugin) {
      is PluginDescriptor.ByInstance -> plugin.plugin
      is PluginDescriptor.ByFile -> PluginManager.getInstance().createPlugin(plugin.file)
      is PluginDescriptor.ByBuildId -> {
        val info = RepositoryManager.getInstance().findUpdateById(plugin.buildId) ?: throw noSuchPlugin(plugin)
        val file = RepositoryManager.getInstance().getPluginFile(info) ?: throw noSuchPlugin(plugin)
        return PluginManager.getInstance().createPlugin(file)
      }
      is PluginDescriptor.ByXmlId -> {
        val suitable: UpdateInfo?
        if (plugin.version != null) {
          val updates = RepositoryManager.getInstance().getAllCompatibleUpdatesOfPlugin(ideVersion, plugin.pluginId)
          suitable = updates.find { plugin.version.equals(it.version) }
        } else {
          suitable = RepositoryManager.getInstance().getLastCompatibleUpdateOfPlugin(ideVersion, plugin.pluginId)
        }
        if (suitable == null) {
          throw noSuchPlugin(plugin)
        }
        val file = RepositoryManager.getInstance().getPluginFile(suitable)
        file ?: throw noSuchPlugin(plugin)
        return PluginManager.getInstance().createPlugin(file)
      }
    }
  }

  private fun noSuchPlugin(plugin: PluginDescriptor): Exception {
    val p: String = when (plugin) {
      is PluginDescriptor.ByBuildId -> plugin.buildId.toString()
      is PluginDescriptor.ByXmlId -> "${plugin.pluginId}${if (plugin.version != null) ":${plugin.version}" else "" }"
      is PluginDescriptor.ByFile -> throw IllegalArgumentException()
      is PluginDescriptor.ByInstance -> throw IllegalArgumentException()
    }
    return IllegalArgumentException("Plugin $p is not found in the Repository")
  }

  fun getIde(ideDescriptor: IdeDescriptor): Ide = when (ideDescriptor) {
    is IdeDescriptor.ByFile -> IdeManager.getInstance().createIde(ideDescriptor.file)
    is IdeDescriptor.ByInstance -> ideDescriptor.ide
  }

}