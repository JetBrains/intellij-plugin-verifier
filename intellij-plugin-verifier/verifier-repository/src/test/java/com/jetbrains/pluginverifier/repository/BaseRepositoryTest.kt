package com.jetbrains.pluginverifier.repository

import com.jetbrains.pluginverifier.plugin.PluginFileProvider
import com.jetbrains.pluginverifier.plugin.PluginFilesBank
import com.jetbrains.pluginverifier.repository.cleanup.DiskSpaceSetting
import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount
import com.jetbrains.pluginverifier.repository.cleanup.fileSize
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.w3c.dom.Document
import org.xml.sax.InputSource
import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Base test for [PluginRepository]s implementations.
 */
abstract class BaseRepositoryTest<R : PluginRepository> {

  @Rule
  @JvmField
  var temporaryFolder = TemporaryFolder()

  protected lateinit var repository: R

  @Before
  fun prepareRepository() {
    repository = createRepository()
  }

  abstract fun createRepository(): R

  fun checkDownloadPlugin(pluginInfo: PluginInfo) {
    val filesBank = PluginFilesBank.create(
        repository,
        temporaryFolder.newFolder().toPath(),
        DiskSpaceSetting(SpaceAmount.ONE_GIGO_BYTE)
    )
    val pluginFileResult = filesBank.getPluginFile(pluginInfo)
    assert(pluginFileResult is PluginFileProvider.Result.Found)
    pluginFileResult as PluginFileProvider.Result.Found
    pluginFileResult.pluginFileLock.use {
      Assert.assertTrue(it.file.fileSize > SpaceAmount.ZERO_SPACE)
    }
  }

  fun parseXmlDocument(xml: String): Document {
    return DocumentBuilderFactory.newInstance()
        .newDocumentBuilder()
        .parse(InputSource(ByteArrayInputStream(xml.toByteArray())))
  }

}