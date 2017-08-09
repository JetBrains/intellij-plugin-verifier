package com.jetbrains.pluginverifier.repository

import org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException
import java.lang.System.getProperty

object RepositoryConfiguration {

  private val DEFAULT_IDE_REPOSITORY_URL = "https://jetbrains.com"

  private val DEFAULT_PLUGIN_REPOSITORY_URL = "https://plugins.jetbrains.com"

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

  val ideRepositoryUrl: String by lazy {
    getProperty("ide.repository.url")?.trimEnd('/') ?: DEFAULT_IDE_REPOSITORY_URL ?: throw RuntimeException("IDE repository URL is not specified")
  }

  val pluginRepositoryUrl: String by lazy {
    getProperty("plugin.repository.url")?.trimEnd('/') ?: DEFAULT_PLUGIN_REPOSITORY_URL ?: throw RuntimeException("Plugin repository URL is not specified")
  }

  val downloadDirMaxSpace: Long? by lazy {
    val property = getProperty("plugin.verifier.cache.dir.max.space")
    return@lazy if (property != null) property.toLong() * FileUtils.ONE_MB else null
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
