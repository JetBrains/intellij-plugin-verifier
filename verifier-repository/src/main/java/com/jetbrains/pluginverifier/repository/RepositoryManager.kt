package com.jetbrains.pluginverifier.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.intellij.structure.domain.IdeVersion
import com.jetbrains.pluginverifier.format.UpdateInfo
import org.apache.commons.io.IOUtils
import org.apache.http.annotation.ThreadSafe
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URL
import java.net.URLEncoder

@ThreadSafe
object RepositoryManager : PluginRepository {

  @Throws(IOException::class)
  override fun getLastCompatibleUpdates(ideVersion: IdeVersion): List<UpdateInfo> {
    LOG.debug("Loading list of plugins compatible with $ideVersion... ")

    val url = URL(RepositoryConfiguration.pluginRepositoryUrl + "/manager/allCompatibleUpdates/?build=" + ideVersion)

    return Gson().fromJson<List<UpdateInfo>>(IOUtils.toString(url, Charsets.UTF_8), updateListType)
  }

  @Throws(IOException::class)
  override fun getLastCompatibleUpdateOfPlugin(ideVersion: IdeVersion, pluginId: String): UpdateInfo? {
    LOG.debug("Fetching last compatible update of plugin {} with ide {}", pluginId, ideVersion)

    //search the given number in the all compatible updates
    val all = getAllCompatibleUpdatesOfPlugin(ideVersion, pluginId)
    var result: UpdateInfo? = null
    for (info in all) {
      if (result == null || result.updateId < info.updateId) {
        result = info
      }
    }

    return result
  }

  @Throws(IOException::class)
  override fun getAllCompatibleUpdatesOfPlugin(ideVersion: IdeVersion, pluginId: String): List<UpdateInfo> {
    LOG.debug("Fetching list of all compatible builds of a pluginId $pluginId on IDE $ideVersion")

    val urlSb = RepositoryConfiguration.pluginRepositoryUrl + "/manager/originalCompatibleUpdatesByPluginIds/?build=" + ideVersion +
        "&pluginIds=" + URLEncoder.encode(pluginId, "UTF-8")

    return Gson().fromJson<List<UpdateInfo>>(IOUtils.toString(URL(urlSb), Charsets.UTF_8), updateListType)
  }

  @Throws(IOException::class)
  override fun getPluginFile(update: UpdateInfo): IFileLock? {
    return getPluginFile(update.updateId)
  }

  @Throws(IOException::class)
  override fun getPluginFile(updateId: Int): IFileLock? {
    return DownloadManager.getOrLoadUpdate(updateId)
  }


  private val LOG = LoggerFactory.getLogger(RepositoryManager::class.java)

  private val updateListType = object : TypeToken<List<UpdateInfo>>() {}.type

}
