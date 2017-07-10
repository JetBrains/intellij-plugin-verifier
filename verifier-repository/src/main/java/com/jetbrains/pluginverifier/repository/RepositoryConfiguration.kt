package com.jetbrains.pluginverifier.repository

import org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException
import java.util.*

object RepositoryConfiguration {

  private val myDefaultProperties: Properties

  init {
    val defaultConfig = Properties()
    try {
      defaultConfig.load(RepositoryConfiguration::class.java.getResourceAsStream("/defaultConfig.properties"))
    } catch (e: IOException) {
      throw RuntimeException("Failed to read defaultConfig.properties", e)
    }

    myDefaultProperties = Properties(defaultConfig)
  }

  private val verifierHomeDir: File
    get() {
      val verifierHomeDir = getProperty("plugin.verifier.home.dir")
      if (verifierHomeDir != null) {
        return File(verifierHomeDir)
      }
      val userHome = getProperty("user.home")
      if (userHome != null) {
        return File(userHome, ".pluginVerifier")
      }
      return File(FileUtils.getTempDirectory(), ".pluginVerifier")
    }

  private fun getProperty(propertyName: String): String? {
    val systemProperty = System.getProperty(propertyName)
    if (systemProperty != null) return systemProperty

    return myDefaultProperties.getProperty(propertyName)
  }

  val ideRepositoryUrl: String by lazy {
    getProperty("ide.repository.url")?.trimEnd('/') ?: throw RuntimeException("IDE repository URL is not specified")
  }

  val pluginRepositoryUrl: String by lazy {
    getProperty("plugin.repository.url")?.trimEnd('/') ?: throw RuntimeException("Plugin repository URL is not specified")
  }

  val downloadDirMaxSpaceMb: Long? by lazy {
    return@lazy getProperty("plugin.verifier.cache.dir.max.space")?.toLong()
  }

  private fun createDir(dir: File): File {
    if (!dir.isDirectory) {
      FileUtils.forceMkdir(dir)
      if (!dir.isDirectory) {
        throw IOException("Failed to create directory $dir")
      }
    }
    return dir
  }

  val downloadDir: File by lazy {
    createDir(File(verifierHomeDir, "cache"))
  }

  val ideDownloadDir: File by lazy {
    createDir(File(verifierHomeDir, "idesCache"))
  }

}
