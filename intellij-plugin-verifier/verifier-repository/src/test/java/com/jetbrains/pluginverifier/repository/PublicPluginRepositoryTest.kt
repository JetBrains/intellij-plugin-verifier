package com.jetbrains.pluginverifier.repository

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.repositories.marketplace.MarketplaceRepository
import com.jetbrains.pluginverifier.repository.repositories.marketplace.UpdateInfo
import org.junit.Assert.*
import org.junit.Test
import java.net.URL

class PublicPluginRepositoryTest : BaseRepositoryTest<MarketplaceRepository>() {

  companion object {
    val repositoryURL = URL("https://plugins.jetbrains.com")
  }

  override fun createRepository() = MarketplaceRepository(repositoryURL)

  @Test
  fun `last compatible plugins for IDE`() {
    val plugins = repository.getLastCompatiblePlugins(IdeVersion.createIdeVersion("173.3727.127"))
    assertFalse(plugins.isEmpty())
  }

  @Test
  fun `browser url for plugin with id containing spaces must be encoded`() {
    val versions = repository.getAllVersionsOfPlugin("Mongo Plugin")
    assertTrue(versions.isNotEmpty())
    val updateInfo = versions.first()
    assertEquals(URL(repositoryURL, "/plugin/index?xmlId=Mongo+Plugin"), updateInfo.browserUrl)
  }

  @Test
  fun updatesOfExistentPlugin() {
    val updates = repository.getAllVersionsOfPlugin("Pythonid")
    assertNotNull(updates)
    assertFalse(updates.isEmpty())
    val update = updates[0]
    assertEquals("Pythonid", update.pluginId)
    assertEquals("JetBrains", update.vendor)

  }

  @Test
  fun updatesOfNonExistentPlugin() {
    val updates = repository.getAllVersionsOfPlugin("NON_EXISTENT_PLUGIN")
    assertEquals(emptyList<UpdateInfo>(), updates)
  }

  @Test
  fun lastUpdate() {
    val info = repository.getLastCompatibleVersionOfPlugin(ideVersion, "org.jetbrains.kotlin")
    assertNotNull(info)
    assertTrue(info!!.updateId > 20000)
  }

  @Test
  fun lastCompatibleUpdates() {
    val updates = repository.getLastCompatiblePlugins(IdeVersion.createIdeVersion("IU-163.2112"))
    assertFalse(updates.isEmpty())
  }

  private val ideVersion: IdeVersion
    get() = IdeVersion.createIdeVersion("182.3040")

  @Test
  fun `find non existent plugin by update id`() {
    val updateInfo = repository.getPluginInfoById(-1000)
    assertNull(updateInfo)
  }

  @Test
  fun downloadExistentPlugin() {
    val updateInfo = repository.getPluginInfoById(40625)!! //.gitignore 2.3.2
    checkDownloadPlugin(updateInfo)
  }

}
