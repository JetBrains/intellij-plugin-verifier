package com.jetbrains.pluginverifier.tests

import com.jetbrains.plugin.structure.base.utils.exists
import com.jetbrains.plugin.structure.base.utils.isDirectory
import java.nio.file.Path
import java.nio.file.Paths

fun findMockIdePath(): Path {
  val afterIdeFile = Paths.get("build", "mocks", "after-idea")
  if (afterIdeFile.exists()) {
    return afterIdeFile
  }
  return Paths.get("verifier-test").resolve(afterIdeFile).also { check(it.exists()) }
}

fun findMockPluginJarPath(): Path {
  val mockPluginFile = Paths.get("build", "mocks", "mock-plugin-1.0.jar")
  if (mockPluginFile.exists()) {
    return mockPluginFile
  }
  return Paths.get("verifier-test").resolve(mockPluginFile).also { check(it.exists()) }
}

fun findMockPluginSourcePath(): Path {
  val mockPluginsSourcePath = Paths.get("mock-plugin", "src", "main", "java")
  if (mockPluginsSourcePath.isDirectory) {
    return mockPluginsSourcePath
  }
  return Paths.get("verifier-test").resolve(mockPluginsSourcePath).also { check(it.isDirectory) }
}

fun findMockPluginKotlinSourcePath(): Path {
  val mockPluginsKotlinSourcePath = Paths.get("mock-plugin", "src", "main", "kotlin")
  if (mockPluginsKotlinSourcePath.isDirectory) {
    return mockPluginsKotlinSourcePath
  }
  return Paths.get("verifier-test").resolve(mockPluginsKotlinSourcePath).also { check(it.isDirectory) }
}
