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

  val pluginRepositoryUrl: String
    get() {
      var res: String = getProperty("plugin.repository.url") ?: throw RuntimeException("Plugin repository URL is not specified")

      if (res.endsWith("/")) {
        res = res.substring(0, res.length - 1)
      }
      return res
    }

  internal val pluginCacheDir: File
    get() = File(verifierHomeDir, "cache")

}
