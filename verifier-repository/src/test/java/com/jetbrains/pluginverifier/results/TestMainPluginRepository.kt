package com.jetbrains.pluginverifier.results

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.PublicPluginRepository
import com.jetbrains.pluginverifier.repository.cleanup.DiskSpaceSetting
import com.jetbrains.pluginverifier.repository.files.FileRepositoryResult
import org.apache.commons.io.FileUtils
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * @author Sergey Patrikeev
 */
class TestMainPluginRepository {

  @Rule
  @JvmField
  var temporaryFolder = TemporaryFolder()

  private fun getRepository(): PluginRepository {
    val tempDownloadFolder = temporaryFolder.newFolder()
    return PublicPluginRepository("https://plugins.jetbrains.com", tempDownloadFolder, DiskSpaceSetting(100 * FileUtils.ONE_MB))
  }

  @Test
  fun updatesOfPlugin() {
    assertTrue(getRepository().getAllCompatibleUpdatesOfPlugin(ideVersion, "ActionScript Profiler").isNotEmpty())
  }

  @Test
  fun updatesOfExistentPlugin() {
    val updates = getRepository().getAllUpdatesOfPlugin("Pythonid")
    assertNotNull(updates)
    assertFalse(updates!!.isEmpty())
    val (pluginId, pluginName, _, _, vendor) = updates[0]
    assertEquals("Pythonid", pluginId)
    assertEquals("Python", pluginName)
    assertEquals("JetBrains", vendor)
  }

  @Test
  fun updatesOfNonExistentPlugin() {
    val updates = getRepository().getAllUpdatesOfPlugin("NON_EXISTENT_PLUGIN")
    assertNull(updates)
  }

  @Test
  fun lastUpdate() {
    val info = getRepository().getLastCompatibleUpdateOfPlugin(ideVersion, "org.jetbrains.kotlin")
    assertNotNull(info)
    assertTrue(info!!.updateId > 20000)
  }

  @Test
  fun lastCompatibleUpdates() {
    val updates = getRepository().getLastCompatibleUpdates(IdeVersion.createIdeVersion("IU-163.2112"))
    assertFalse(updates.isEmpty())
  }

  private val ideVersion: IdeVersion
    get() = IdeVersion.createIdeVersion("IU-162.1132.10")

  @Test
  fun downloadNonExistentPlugin() {
    val updateInfo = getRepository().getUpdateInfoById(-1000)
    assertNull(updateInfo)
  }

  @Test
  fun downloadExistentPlugin() {
    val updateInfo = getRepository().getUpdateInfoById(25128) //.gitignore 1.3.3
    assertNotNull(updateInfo)
    val downloadPluginResult = getRepository().downloadPluginFile(updateInfo!!)
    assertTrue(downloadPluginResult is FileRepositoryResult.Found)
    val fileLock = (downloadPluginResult as FileRepositoryResult.Found).lockedFile
    assertNotNull(fileLock)
    assertTrue(fileLock.file.length() > 0)
    fileLock.release()
  }

}
