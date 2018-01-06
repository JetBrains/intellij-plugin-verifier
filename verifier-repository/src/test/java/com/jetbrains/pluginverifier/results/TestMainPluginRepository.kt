package com.jetbrains.pluginverifier.results

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.PublicPluginRepository
import com.jetbrains.pluginverifier.repository.UpdateInfo
import com.jetbrains.pluginverifier.repository.cleanup.DiskSpaceSetting
import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount
import com.jetbrains.pluginverifier.repository.cleanup.fileSize
import com.jetbrains.pluginverifier.repository.files.FileRepositoryResult
import org.junit.Assert.*
import org.junit.Before
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

  private lateinit var repository: PluginRepository

  @Before
  fun prepareRepository() {
    val tempDownloadFolder = temporaryFolder.newFolder().toPath()
    repository = PublicPluginRepository("https://plugins.jetbrains.com", tempDownloadFolder, DiskSpaceSetting(SpaceAmount.ofMegabytes(100)))
  }

  @Test
  fun updatesOfPlugin() {
    assertTrue(repository.getAllCompatibleVersionsOfPlugin(ideVersion, "ActionScript Profiler").isNotEmpty())
  }

  @Test
  fun updatesOfExistentPlugin() {
    val updates = repository.getAllVersionsOfPlugin("Pythonid")
    assertNotNull(updates)
    assertFalse(updates.isEmpty())
    val update = updates[0] as UpdateInfo
    assertEquals("Pythonid", update.pluginId)
    assertEquals("Python", update.pluginName)
    assertEquals("JetBrains", update.vendor)
  }

  @Test
  fun updatesOfNonExistentPlugin() {
    val updates = repository.getAllVersionsOfPlugin("NON_EXISTENT_PLUGIN")
    assertEquals(emptyList<UpdateInfo>(), updates)
  }

  @Test
  fun lastUpdate() {
    val info = repository.getLastCompatibleVersionOfPlugin(ideVersion, "org.jetbrains.kotlin") as? UpdateInfo
    assertNotNull(info)
    assertTrue(info!!.updateId > 20000)
  }

  @Test
  fun lastCompatibleUpdates() {
    val updates = repository.getLastCompatiblePlugins(IdeVersion.createIdeVersion("IU-163.2112"))
    assertFalse(updates.isEmpty())
  }

  private val ideVersion: IdeVersion
    get() = IdeVersion.createIdeVersion("IU-162.1132.10")

  @Test
  fun downloadNonExistentPlugin() {
    val updateInfo = repository.getPluginInfoById(-1000)
    assertNull(updateInfo)
  }

  @Test
  fun downloadExistentPlugin() {
    val updateInfo = repository.getPluginInfoById(40625) //.gitignore 2.3.2
    assertNotNull(updateInfo)
    val downloadPluginResult = repository.downloadPluginFile(updateInfo!!)
    assertTrue(downloadPluginResult is FileRepositoryResult.Found)
    val fileLock = (downloadPluginResult as FileRepositoryResult.Found).lockedFile
    assertNotNull(fileLock)
    assertTrue(fileLock.file.fileSize > SpaceAmount.ZERO_SPACE)
    fileLock.release()
  }

}
