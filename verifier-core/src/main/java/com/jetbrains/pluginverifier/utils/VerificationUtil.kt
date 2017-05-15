package com.jetbrains.pluginverifier.utils

import com.intellij.structure.domain.*
import com.intellij.structure.errors.IncorrectPluginException
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.*
import com.jetbrains.pluginverifier.misc.closeLogged
import com.jetbrains.pluginverifier.repository.FileLock
import com.jetbrains.pluginverifier.repository.RepositoryManager
import org.slf4j.LoggerFactory
import java.io.Closeable

object VerificationUtil {

  private val LOG = LoggerFactory.getLogger(Verifier::class.java)

  data class IdeCreateResult(val ide: Ide, val ideResolver: Resolver, private val closeResolver: Boolean) : Closeable {
    override fun close() {
      if (closeResolver) {
        ideResolver.closeLogged()
      }
    }
  }

  data class CreatePluginResult(val plugin: Plugin? = null,
                                val pluginResolver: Resolver? = null,
                                val bad: Result? = null,
                                private val closeResolver: Boolean = false,
                                private val fileLock: FileLock? = null) : Closeable {
    override fun close() {
      if (bad == null) {
        try {
          if (closeResolver) {
            pluginResolver?.closeLogged()
          }
        } finally {
          fileLock?.release()
        }
      }
    }
  }

  fun createIdeAndResolver(ideDescriptor: IdeDescriptor): IdeCreateResult = when (ideDescriptor) {
    is IdeDescriptor.ByInstance -> IdeCreateResult(ideDescriptor.ide, ideDescriptor.ideResolver, false)
    is IdeDescriptor.ByFile -> {
      val ide = IdeManager.getInstance().createIde(ideDescriptor.file)
      val ideResolver = Resolver.createIdeResolver(ide)
      IdeCreateResult(ide, ideResolver, true)
    }
  }

  fun createPluginAndResolver(pluginDescriptor: PluginDescriptor, ideVersion: IdeVersion): CreatePluginResult = when (pluginDescriptor) {
    is PluginDescriptor.ByInstance -> CreatePluginResult(pluginDescriptor.plugin, pluginDescriptor.resolver, closeResolver = false, fileLock = null)
    is PluginDescriptor.ByFileLock -> createPluginByFileLock(pluginDescriptor, pluginDescriptor.fileLock, ideVersion)
    is PluginDescriptor.ByUpdateInfo -> createPluginByUpdateInfo(pluginDescriptor, ideVersion)
  }

  private fun createPluginByUpdateInfo(pluginDescriptor: PluginDescriptor.ByUpdateInfo, ideVersion: IdeVersion): CreatePluginResult {
    val updateInfo = pluginDescriptor.updateInfo
    val pluginInfo = getPluginInfoByDescriptor(pluginDescriptor)
    val fileLock: FileLock? = try {
      RepositoryManager.getPluginFile(updateInfo)
    } catch (e: InterruptedException) {
      throw e
    } catch (e: Exception) {
      val reason = "Unable to download plugin $updateInfo from the Plugin Repository"
      LOG.debug(reason, e)
      return badPluginResult(reason, ideVersion, pluginInfo)
    }

    if (fileLock == null) {
      val reason = "Plugin $pluginDescriptor is not found the Plugin Repository"
      return badPluginResult(reason, ideVersion, pluginInfo)
    }
    return createPluginByFileLock(pluginDescriptor, fileLock, ideVersion)
  }

  private fun getPluginInfoByDescriptor(pluginDescriptor: PluginDescriptor): PluginInfo = when (pluginDescriptor) {
    is PluginDescriptor.ByUpdateInfo -> PluginInfo(pluginDescriptor.pluginId, pluginDescriptor.version, pluginDescriptor.updateInfo)
    else -> PluginInfo(pluginDescriptor.pluginId, pluginDescriptor.version, null)
  }

  private fun createPluginByFileLock(pluginDescriptor: PluginDescriptor,
                                     fileLock: FileLock,
                                     ideVersion: IdeVersion): CreatePluginResult {
    val pluginInfo = getPluginInfoByDescriptor(pluginDescriptor)

    var plugin: Plugin? = null
    try {
      try {
        plugin = PluginManager.getInstance().createPlugin(fileLock.getFile())
      } catch (e: InterruptedException) {
        throw e
      } catch (e: IncorrectPluginException) {
        return badPluginResult(e.message ?: "Invalid structure: $pluginDescriptor", ideVersion, pluginInfo)
      } catch (e: Exception) {
        return badPluginResult("Unable to read plugin $pluginDescriptor", ideVersion, pluginInfo)
      }
    } finally {
      if (plugin == null) {
        fileLock.release()
      }
    }

    var pluginResolver: Resolver? = null
    try {
      try {
        pluginResolver = Resolver.createPluginResolver(plugin)
      } catch (e: Exception) {
        return badPluginResult("Unable to read class files $pluginDescriptor", ideVersion, pluginInfo)
      }
    } finally {
      if (pluginResolver == null) {
        fileLock.release()
      }
    }

    return CreatePluginResult(plugin, pluginResolver, closeResolver = true, fileLock = fileLock)
  }

  private fun badPluginResult(reason: String, ideVersion: IdeVersion, pluginInfo: PluginInfo): CreatePluginResult =
      CreatePluginResult(bad = Result(pluginInfo, ideVersion, Verdict.Bad(reason)))

}
